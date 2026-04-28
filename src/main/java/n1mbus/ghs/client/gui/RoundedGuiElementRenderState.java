package n1mbus.ghs.client.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/**
 * 1.21.3以降ではSpecialGuiElementRenderStateが削除されたため、
 * 単なるデータ保持用のrecordとして使用します。
 */
public record RoundedGuiElementRenderState(
    int x1,
    int y1,
    int x2,
    int y2,
    float cornerRadius,
    int color,
    Matrix3x2f pose,
    @Nullable ScreenRectangle scissorArea,
    float smoothing
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
