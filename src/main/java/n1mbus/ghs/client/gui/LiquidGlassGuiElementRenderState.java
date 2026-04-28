package n1mbus.ghs.client.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import n1mbus.ghs.client.api.WidgetStyle;

/**
 * Data record for LiquidGlass rendering state.
 */
public record LiquidGlassGuiElementRenderState(
    int x1,
    int y1,
    int x2,
    int y2,
    float cornerRadius,
    float hover,
    float focus,
    WidgetStyle style,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea
) {
    public float scale() {
        return 1.0f;
    }

    @Nullable
    public ScreenRectangle bounds() {
        ScreenRectangle ownBounds = new ScreenRectangle(x1, y1, x2 - x1, y2 - y1);
        return scissorArea != null ? scissorArea.intersection(ownBounds) : ownBounds;
    }
}