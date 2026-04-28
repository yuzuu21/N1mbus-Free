package n1mbus.ghs.client.gui.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import n1mbus.ghs.client.gui.RoundedUiRenderer;
import n1mbus.ghs.client.LiquidGlassUniforms;
import n1mbus.ghs.client.gui.LiquidGlassGuiElementRenderState;
import n1mbus.ghs.client.api.WidgetStyle;
import org.joml.Matrix3x2fStack;

public final class CustomUiRenderer {
    private CustomUiRenderer() {}

    public static void addWidget(int x, int y, int width, int height, float radius, int color, Matrix3x2fStack matrices) {
        WidgetStyle style = WidgetStyle.create()
            .tint(color, ((color >> 24) & 0xFF) / 255f);
        
        LiquidGlassGuiElementRenderState state = new LiquidGlassGuiElementRenderState(
            x, y, x + width, y + height, radius, 0f, 0f, style, matrices, null
        );
        LiquidGlassUniforms.get().addWidget(state);
    }

    public static void render(GuiGraphics context, CustomUiTheme theme) {
        if (theme == null || theme.layout() == null) return;

        for (CustomUiTheme.ComponentConfig comp : theme.layout()) {
            switch (comp.type()) {
                case "image" -> renderImage(context, comp);
                case "panel" -> renderPanel(context, comp);
                // Future: add more types like "text", "progress", etc.
            }
        }
    }

    private static void renderImage(GuiGraphics context, CustomUiTheme.ComponentConfig comp) {
        String path = (String) comp.properties().get("path");
        if (path == null) return;

        ResourceLocation id = CustomUiManager.get().getTexture(path);
        if (id != null) {
            context.blit(id, comp.x(), comp.y(), 0, 0, comp.width(), comp.height(), comp.width(), comp.height());
        }
    }

    private static void renderPanel(GuiGraphics context, CustomUiTheme.ComponentConfig comp) {
        String colorStr = (String) comp.properties().getOrDefault("color", "#FFFFFF");
        int color = parseHex(colorStr);
        Object radiusObj = comp.properties().getOrDefault("radius", 4.0);
        float radius = radiusObj instanceof Number ? ((Number) radiusObj).floatValue() : 4.0f;
        
        RoundedUiRenderer.drawRoundedRect(context, comp.x(), comp.y(), comp.width(), comp.height(), radius, color);
    }

    public static int parseHex(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() == 6) return 0xFF000000 | (int) Long.parseLong(hex, 16);
            if (hex.length() == 8) {
                // Support both ARGB (standard for many MC mods) and handle potentially large values
                return (int) Long.parseUnsignedLong(hex, 16);
            }
            return (int) Long.parseLong(hex, 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }
}
