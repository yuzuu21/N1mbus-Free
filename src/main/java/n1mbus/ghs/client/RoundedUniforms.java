package n1mbus.ghs.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import n1mbus.ghs.client.gui.RoundedGuiElementRenderState;

public final class RoundedUniforms {
    private static final RoundedUniforms INSTANCE = new RoundedUniforms();
    private static final int MAX_WIDGETS = 128;

    private final GpuBuffer samplerInfo;
    private final GpuBuffer roundedInfo;
    private final List<RoundedGuiElementRenderState> widgets = new ArrayList<>();

    public static RoundedUniforms get() {
        return INSTANCE;
    }

    private RoundedUniforms() {
        samplerInfo = RenderSystem.getDevice().createBuffer(() -> "nimbus RoundedSamplerInfo", 130, 16);

        Std140SizeCalculator calc = new Std140SizeCalculator();
        calc.putFloat();
        calc.align(16);
        for (int i = 0; i < MAX_WIDGETS; i++) calc.putVec4();
        for (int i = 0; i < MAX_WIDGETS; i++) calc.putVec4();
        for (int i = 0; i < MAX_WIDGETS; i++) calc.putVec4();
        for (int i = 0; i < MAX_WIDGETS; i++) calc.putVec4();
        roundedInfo = RenderSystem.getDevice().createBuffer(() -> "nimbus RoundedInfo", 130, calc.get());
    }

    public void beginFrame() {
        widgets.clear();
    }

    public void addWidget(RoundedGuiElementRenderState element) {
        if (widgets.size() < MAX_WIDGETS) {
            widgets.add(element);
        }
    }

    public int getCount() {
        return widgets.size();
    }

    public GpuBuffer getSamplerInfoBuffer() {
        return samplerInfo;
    }

    public GpuBuffer getRoundedInfoBuffer() {
        return roundedInfo;
    }

    public void upload() {
        Minecraft mc = Minecraft.getInstance();
        int outW = mc.getMainRenderTarget().width;
        int outH = mc.getMainRenderTarget().height;
        float scale = (float) mc.getWindow().getGuiScale();

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(samplerInfo, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putVec2((float) outW, (float) outH);
            b.putVec2((float) outW, (float) outH);
        }

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(roundedInfo, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putFloat((float) widgets.size());
            b.align(16);

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    ScreenRectangle bounds = widgets.get(i).bounds();
                    if (bounds != null) {
                        float left = bounds.left() * scale;
                        float right = bounds.right() * scale;
                        float top = bounds.top() * scale;
                        float bottom = bounds.bottom() * scale;
                        b.putVec4(left, outH - bottom, right - left, bottom - top);
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                } else {
                    b.putVec4(0f, 0f, 0f, 0f);
                }
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    int c = widgets.get(i).color();
                    b.putVec4(
                        ARGB.red(c) / 255f,
                        ARGB.green(c) / 255f,
                        ARGB.blue(c) / 255f,
                        ARGB.alpha(c) / 255f
                    );
                } else {
                    b.putVec4(0f, 0f, 0f, 0f);
                }
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    ScreenRectangle bounds = widgets.get(i).bounds();
                    float radius = widgets.get(i).cornerRadius() * scale;
                    float width = bounds != null ? bounds.width() * scale : 0f;
                    float height = bounds != null ? bounds.height() * scale : 0f;
                    // Keep corners "rounded-rect" instead of drifting into capsule shapes on short rows.
                    float radiusCap = 0.34f * Math.min(width, height);
                    float clampedRadius = Math.max(0f, Math.min(radius, radiusCap));
                    float smoothing = widgets.get(i).smoothing() * scale;
                    b.putVec4(clampedRadius, 1.25f, smoothing, 0f);
                } else {
                    b.putVec4(0f, 0f, 0f, 0f);
                }
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    ScreenRectangle sc = widgets.get(i).scissorArea();
                    if (sc != null) {
                        float sL = sc.left() * scale;
                        float sR = sc.right() * scale;
                        float sT = sc.top() * scale;
                        float sB = sc.bottom() * scale;
                        b.putVec4(sL, outH - sB, sR, outH - sT);
                    } else {
                        b.putVec4(0f, 0f, outW, outH);
                    }
                } else {
                    b.putVec4(0f, 0f, 0f, 0f);
                }
            }
        }
    }
}
