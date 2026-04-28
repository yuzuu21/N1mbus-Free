package n1mbus.ghs.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import n1mbus.ghs.client.module.ModuleManager;
import n1mbus.ghs.client.module.modules.visual.ESP;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Esp3DWorldRenderer {
    private static boolean initialized = false;
    private static long lastLogMs = 0L;
    private static long lastFrameNs = System.nanoTime();
    private static final java.util.Map<Integer, AABB> SMOOTHED = new java.util.HashMap<>();
    private static final java.util.Set<Integer> VISIBLE = new java.util.HashSet<>();

    private Esp3DWorldRenderer() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        System.out.println("[NimbusGhs] 3D ESP renderer initialized.");
    }

    public static void render(PoseStack matrices, Camera camera, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || camera == null) return;

        ESP esp = ModuleManager.INSTANCE.getModule(ESP.class);
        if (esp == null || !esp.isEnabled() || !"3D Box".equals(esp.getType())) return;

        Vec3 camPos = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        long nowNs = System.nanoTime();
        double dt = Math.min(0.10, Math.max(0.0, (nowNs - lastFrameNs) / 1_000_000_000.0));
        lastFrameNs = nowNs;
        double smoothT = smoothingAlpha(dt, 20.0);

        int rendered = 0;
        VISIBLE.clear();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!esp.shouldRender(entity)) continue;
            if (entity == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            int entityId = entity.getId();
            VISIBLE.add(entityId);

            Vec3 interp = entity.getPosition(partialTicks);
            double halfW = (entity.getBbWidth() * 0.5) + 0.03;
            double h = entity.getBbHeight() + 0.05;
            AABB target = new AABB(
                interp.x - halfW, interp.y, interp.z - halfW,
                interp.x + halfW, interp.y + h, interp.z + halfW
            );
            AABB prev = SMOOTHED.get(entityId);
            AABB bb = prev == null ? target : smooth(prev, target, Math.min(0.86, smoothT));
            SMOOTHED.put(entityId, bb);

            int rgb = esp.getEspColor(entity);
            float r = ((rgb >> 16) & 0xFF) / 255.0f;
            float g = ((rgb >> 8) & 0xFF) / 255.0f;
            float b = (rgb & 0xFF) / 255.0f;

            ShapeRenderer.renderLineBox(
                matrices,
                vc,
                (float) (bb.minX - camPos.x), (float) (bb.minY - camPos.y), (float) (bb.minZ - camPos.z),
                (float) (bb.maxX - camPos.x), (float) (bb.maxY - camPos.y), (float) (bb.maxZ - camPos.z),
                r, g, b, 1.0f
            );
            rendered++;
        }
        SMOOTHED.keySet().removeIf(id -> !VISIBLE.contains(id));

        bufferSource.endBatch(RenderType.lines());

        long now = System.currentTimeMillis();
        if (now - lastLogMs > 2000) {
            lastLogMs = now;
            System.out.println("[NimbusGhs] 3D ESP Debug rendered=" + rendered);
        }
    }

    private static AABB smooth(AABB a, AABB b, double t) {
        double ax = (a.minX + a.maxX) * 0.5;
        double ay = (a.minY + a.maxY) * 0.5;
        double az = (a.minZ + a.maxZ) * 0.5;
        double bx = (b.minX + b.maxX) * 0.5;
        double by = (b.minY + b.maxY) * 0.5;
        double bz = (b.minZ + b.maxZ) * 0.5;
        double distSq = (bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az);
        if (distSq > 9.0) return b;

        return new AABB(
            lerp(a.minX, b.minX, t),
            lerp(a.minY, b.minY, t),
            lerp(a.minZ, b.minZ, t),
            lerp(a.maxX, b.maxX, t),
            lerp(a.maxY, b.maxY, t),
            lerp(a.maxZ, b.maxZ, t)
        );
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothingAlpha(double dt, double responsiveness) {
        // Exponential smoothing with frame-rate independent behavior.
        return 1.0 - Math.exp(-responsiveness * dt);
    }
}
