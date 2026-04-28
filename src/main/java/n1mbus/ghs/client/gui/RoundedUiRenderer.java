package n1mbus.ghs.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import n1mbus.ghs.client.RoundedUniforms;
import n1mbus.ghs.client.accessor.GuiGraphicsAccessor;

public final class RoundedUiRenderer {
    private RoundedUiRenderer() {}

    public static void drawCircle(GuiGraphics context, int x, int y, int size, int color) {
        drawRoundedRect(context, x, y, size, size, size * 0.5f, color);
    }

    public static void drawCapsule(GuiGraphics context, int x, int y, int width, int height, int color) {
        drawRoundedRect(context, x, y, width, height, Math.min(width, height) * 0.5f, color);
    }

    public static void drawRoundedRect(GuiGraphics context, int x, int y, int width, int height, float radius, int color) {
        drawLiquidRect(context, x, y, width, height, radius, 0.0f, color);
    }

    public static void drawLiquidRect(GuiGraphics context, int x, int y, int width, int height, float radius, float smoothing, int color) {
        if (((color >>> 24) & 0xFF) == 0 || width <= 0 || height <= 0) {
            return;
        }

        Matrix3x2f pose = new Matrix3x2f(context.pose());
        ScreenRectangle scissorRect = ((n1mbus.ghs.client.accessor.GuiGraphicsAccessor) context).n1mbusghs$peekScissor();
        
        // MinecraftのSpecialGuiElementを使わず、直接独自のUniformsシステムへ登録する
        RoundedUniforms.get().addWidget(new RoundedGuiElementRenderState(
            x,
            y,
            x + width,
            y + height,
            Math.max(0f, radius),
            color,
            pose,
            scissorRect,
            smoothing
        ));
    }
}
