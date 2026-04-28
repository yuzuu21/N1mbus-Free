package n1mbus.ghs.mixin.logical;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import n1mbus.ghs.client.module.ModuleManager;
import n1mbus.ghs.client.module.modules.visual.ESP;
import n1mbus.ghs.util.render.RenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    private static final java.util.Map<Integer, SmoothRect> ESP_SMOOTH = new java.util.HashMap<>();
    private static final java.util.Set<Integer> ESP_VISIBLE_THIS_FRAME = new java.util.HashSet<>();

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        ESP esp = (ESP) ModuleManager.INSTANCE.getModules().stream().filter(m -> m.getName().equals("ESP")).findFirst().orElse(null);
        
        if (esp != null && esp.isEnabled() && esp.getType().equals("Box")) {
            float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
            ESP_VISIBLE_THIS_FRAME.clear();
            
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (esp.shouldRender(entity)) {
                    renderBoxEsp(guiGraphics, esp, entity, partialTicks);
                }
            }

            ESP_SMOOTH.keySet().removeIf(id -> !ESP_VISIBLE_THIS_FRAME.contains(id));
        } else {
            ESP_SMOOTH.clear();
        }
    }

    private void renderBoxEsp(GuiGraphics graphics, ESP esp, Entity entity, float partialTicks) {
        Vec3[] corners = projectableCorners(entity, partialTicks);

        boolean is3d = esp.getType().equals("3D Box");

        Vec3[] p = new Vec3[8];
        int projected = 0;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            p[i] = RenderUtil.project(corners[i]);
            if (p[i] != null) {
                projected++;
                minX = Math.min(minX, (float) p[i].x);
                minY = Math.min(minY, (float) p[i].y);
                maxX = Math.max(maxX, (float) p[i].x);
                maxY = Math.max(maxY, (float) p[i].y);
            }
        }

        if (projected < 1) return;

        int color = esp.getEspColor(entity) | 0xFF000000;

        if (is3d) return;

        if (projected < 2) return;
        float boxHeight = maxY - minY;
        if (boxHeight <= 1.0f) return;

        int id = entity.getId();
        ESP_VISIBLE_THIS_FRAME.add(id);
        SmoothRect rect = ESP_SMOOTH.get(id);
        if (rect == null) {
            rect = new SmoothRect(minX, minY, maxX, maxY);
            ESP_SMOOTH.put(id, rect);
        }
        rect.smoothTo(minX, minY, maxX, maxY, 0.62f);

        drawRect(graphics, rect.left, rect.top, rect.right, rect.bottom, color, 1.0f);

        if (entity instanceof LivingEntity living) {
            float healthPerc = Math.max(0.0f, Math.min(1.0f, living.getHealth() / living.getMaxHealth()));
            int healthColor = getHealthColor(healthPerc);
            float smoothedHeight = rect.bottom - rect.top;
            float barHeight = smoothedHeight * healthPerc;
            graphics.fill((int)(rect.left - 4), (int)(rect.bottom - barHeight), (int)(rect.left - 2), (int)rect.bottom, healthColor);
        }
    }

    private void drawRect(GuiGraphics g, float x, float y, float x2, float y2, int color, float thickness) {
        g.fill((int)x, (int)y, (int)x2, (int)(y + thickness), color);
        g.fill((int)x, (int)(y2 - thickness), (int)x2, (int)y2, color);
        g.fill((int)x, (int)y, (int)(x + thickness), (int)y2, color);
        g.fill((int)(x2 - thickness), (int)y, (int)x2, (int)y2, color);
    }


    private Vec3[] projectableCorners(Entity entity, float partialTicks) {
        Vec3 interpolatedPos = entity.getPosition(partialTicks);
        Vec3 currentPos = entity.position();
        Vec3 delta = interpolatedPos.subtract(currentPos);
        AABB box = entity.getBoundingBox().move(delta.x, delta.y, delta.z).inflate(0.03);
        return new Vec3[] {
            new Vec3(box.minX, box.minY, box.minZ),
            new Vec3(box.minX, box.minY, box.maxZ),
            new Vec3(box.minX, box.maxY, box.minZ),
            new Vec3(box.minX, box.maxY, box.maxZ),
            new Vec3(box.maxX, box.minY, box.minZ),
            new Vec3(box.maxX, box.minY, box.maxZ),
            new Vec3(box.maxX, box.maxY, box.minZ),
            new Vec3(box.maxX, box.maxY, box.maxZ)
        };
    }

    private int getHealthColor(float perc) {
        if (perc > 0.75f) return 0xFF00FF00;
        if (perc > 0.5f) return 0xFFFFFF00;
        if (perc > 0.25f) return 0xFFFF7F00;
        return 0xFFFF0000;
    }

    private static final class SmoothRect {
        float left;
        float top;
        float right;
        float bottom;

        SmoothRect(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        void smoothTo(float l, float t, float r, float b, float alpha) {
            float dx = Math.max(Math.abs(l - this.left), Math.abs(r - this.right));
            float dy = Math.max(Math.abs(t - this.top), Math.abs(b - this.bottom));

            // Priority to tracking if jump is too large.
            if (dx > 26.0f || dy > 26.0f) {
                this.left = l;
                this.top = t;
                this.right = r;
                this.bottom = b;
                return;
            }

            this.left += (l - this.left) * alpha;
            this.top += (t - this.top) * alpha;
            this.right += (r - this.right) * alpha;
            this.bottom += (b - this.bottom) * alpha;
        }
    }
}
