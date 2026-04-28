package n1mbus.ghs.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import n1mbus.ghs.client.api.N1mbusGhsConfig;
import n1mbus.ghs.client.api.WidgetStyle;
import n1mbus.ghs.client.gui.LiquidGlassGuiElementRenderState;
import n1mbus.ghs.client.runtime.N1mbusGhsAnim;

public final class LiquidGlassUniforms {

    private static final LiquidGlassUniforms INSTANCE = new LiquidGlassUniforms();
    public static final int MAX_WIDGETS = 64;
    public static final int MAX_BLUR_LEVELS = 5;

    public static LiquidGlassUniforms get() { return INSTANCE; }

    private final GpuBuffer samplerInfo;
    private final GpuBuffer customUniforms;
    private final GpuBuffer widgetInfo;
    private final GpuBuffer bgConfig;
    private final GpuBuffer projectionBuffer;

    private final List<LiquidGlassGuiElementRenderState> widgets = new ArrayList<>();
    private boolean screenWantsBlur = false;

    private List<Integer> usedBlurRadiiOrdered = new ArrayList<>();
    private final HashMap<Integer, Integer> blurRadiusToIndex = new HashMap<>();

    private static final class FadeState {
        float hover;
        float focus;
    }

    private final HashMap<Long, FadeState> fades = new HashMap<>();
    private double dtSeconds = 0.0;

    private LiquidGlassUniforms() {
        var device = RenderSystem.getDevice();
        samplerInfo = device.createBuffer(() -> "nimbus SamplerInfo", 130, 16);

        Std140SizeCalculator calc = new Std140SizeCalculator();
        calc.putFloat(); // time
        calc.align(16);
        calc.putVec4(); // mouse
        calc.putFloat(); // blur
        calc.align(16);
        calc.putVec3(); // rimLightDir
        calc.align(16);
        calc.putVec4(); // rimLightColor
        calc.putFloat(); // pixelEpsilon
        calc.putFloat(); // debugStep
        calc.putFloat(); // pixelatedGrid
        calc.putFloat(); // pixelatedGridSize
        calc.putFloat(); // hoverScale
        calc.putFloat(); // focusScale
        calc.putFloat(); // focusBorderWidth
        calc.putFloat(); // focusBorderIntensity
        calc.putFloat(); // focusBorderSpeed
        customUniforms = device.createBuffer(() -> "nimbus CustomUniforms", 130, calc.get());

        int widgetInfoSize = 16 + MAX_WIDGETS * (16 * 12);
        widgetInfo = device.createBuffer(() -> "nimbus WidgetInfo", 130, widgetInfoSize);

        Std140SizeCalculator bcalc = new Std140SizeCalculator();
        bcalc.putFloat(); // shadowExpand
        bcalc.putFloat(); // shadowFactor
        bcalc.putVec2(); // shadowOffset
        bgConfig = device.createBuffer(() -> "nimbus BgConfig", 130, bcalc.get());

        projectionBuffer = device.createBuffer(() -> "nimbus Projection", 130, 64);
    }

    public void updateTime(double dtSeconds) {
        this.dtSeconds = Math.max(0.0, dtSeconds);
    }

    public void beginFrame(double dtSeconds) {
        widgets.clear();
        screenWantsBlur = false;
        usedBlurRadiiOrdered.clear();
        blurRadiusToIndex.clear();
        if (dtSeconds > 0) this.dtSeconds = dtSeconds;
    }

    public void setScreenWantsBlur(boolean wantsBlur) { this.screenWantsBlur = wantsBlur; }

    public void uploadSharedUniforms() {
        Minecraft mc = Minecraft.getInstance();
        int outW = mc.getMainRenderTarget().width;
        int outH = mc.getMainRenderTarget().height;

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(samplerInfo, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putVec2((float) outW, (float) outH);
            b.putVec2((float) outW, (float) outH);
        }

        double[] mx = new double[1];
        double[] my = new double[1];
        GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), mx, my);
        float scale = (float) mc.getWindow().getGuiScale();
        int fbH = mc.getMainRenderTarget().height;

        float time = (float) GLFW.glfwGetTime();
        N1mbusGhsConfig config = N1mbusGhsConfig.INSTANCE;

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(customUniforms, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putFloat(time);
            b.align(16);
            float x = (float) (mx[0] * scale);
            float y = fbH - (float) (my[0] * scale);
            b.putVec4(new Vector4f(x, y, 0f, 0f));
            b.putFloat(this.screenWantsBlur ? 1.0f : 0.0f);
            b.align(16);
            var dir2 = config.rimLight.direction();
            b.putVec3(new Vector3f(dir2.x, dir2.y, 0.0f));
            b.align(16);
            int rc = config.rimLight.color();
            b.putVec4(ARGB.red(rc) / 255f, ARGB.green(rc) / 255f, ARGB.blue(rc) / 255f, config.rimLight.intensity());
            b.putFloat(config.pixelEpsilon);
            b.putFloat(N1mbusGhsAnim.INSTANCE.debugStep());
            b.putFloat(config.features.pixelatedGrid ? 1.0f : 0.0f);
            b.putFloat(N1mbusGhsAnim.INSTANCE.pixelatedGridSize());
            b.putFloat(N1mbusGhsAnim.INSTANCE.hoverScalePx());
            b.putFloat(N1mbusGhsAnim.INSTANCE.focusScalePx());
            b.putFloat(N1mbusGhsAnim.INSTANCE.focusBorderWidthPx());
            b.putFloat(N1mbusGhsAnim.INSTANCE.focusBorderIntensity());
            b.putFloat(N1mbusGhsAnim.INSTANCE.focusBorderSpeed());
        }

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(bgConfig, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putFloat(N1mbusGhsAnim.INSTANCE.shadowExpand());
            b.putFloat(N1mbusGhsAnim.INSTANCE.shadowFactor());
            float s = (float) mc.getWindow().getGuiScale();
            b.putVec2(N1mbusGhsAnim.INSTANCE.shadowOffsetX() * s, N1mbusGhsAnim.INSTANCE.shadowOffsetY() * s);
        }
    }
    
    public void uploadProjectionMatrix(Matrix4f mat) {
        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(projectionBuffer, false, true)) {
            mat.get(map.data().asFloatBuffer());
        }
    }

    public void addWidget(LiquidGlassGuiElementRenderState element) {
        if (widgets.size() >= MAX_WIDGETS) return;
        widgets.add(element);
    }

    private static long rectKey(int x1, int y1, int x2, int y2) {
        long a = (((long) x1) & 0xFFFFFFFFL) | ((((long) y1) & 0xFFFFFFFFL) << 32);
        long b = (((long) x2) & 0xFFFFFFFFL) | ((((long) y2) & 0xFFFFFFFFL) << 32);
        long h = 1469598103934665603L;
        h ^= a; h *= 1099511628211L;
        h ^= b; h *= 1099511628211L;
        return h;
    }

    private float smoothToward(float current, float target, double dt, float tau) {
        if (tau <= 1e-5f) return target;
        float a = (float) (1.0 - Math.exp(-Math.max(0.0, dt) / Math.max(1e-4, tau)));
        float v = current + (target - current) * a;
        if (Math.abs(v - target) < 1e-4f) return target;
        return v;
    }

    public void uploadWidgetInfo() {
        Minecraft mc = Minecraft.getInstance();
        int fbH = mc.getMainRenderTarget().height;
        float scale = (float) mc.getWindow().getGuiScale();

        HashSet<Integer> requested = new HashSet<>();
        for (LiquidGlassGuiElementRenderState w : widgets) {
            WidgetStyle s = w.style();
            requested.add(Math.max(0, s.getBlurRadius()));
        }
        List<Integer> sorted = requested.stream().sorted().toList();
        usedBlurRadiiOrdered = new ArrayList<>();
        for (int i = 0; i < sorted.size() && i < MAX_BLUR_LEVELS; i++) usedBlurRadiiOrdered.add(sorted.get(i));
        if (usedBlurRadiiOrdered.isEmpty()) usedBlurRadiiOrdered.add(N1mbusGhsAnim.INSTANCE.blurRadiusInt());
        blurRadiusToIndex.clear();
        for (int i = 0; i < usedBlurRadiiOrdered.size(); i++) blurRadiusToIndex.put(usedBlurRadiiOrdered.get(i), i);

        try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(widgetInfo, false, true)) {
            Std140Builder b = Std140Builder.intoBuffer(map.data());
            b.putFloat((float) widgets.size());
            b.align(16);

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    var w = widgets.get(i);
                    var pose = w.pose();
                    float W = w.x2() - w.x1();
                    float H = w.y2() - w.y1();
                    
                    float tx = pose.m00 * w.x1() + pose.m10 * w.y1() + pose.m20;
                    float ty = pose.m01 * w.x1() + pose.m11 * w.y1() + pose.m21;
                    
                    float px = tx * scale;
                    float pyTop = ty * scale;
                    float pW = W * scale;
                    float pH = H * scale;
                    
                    float cx = px + 0.5f * pW;
                    float cyTop = pyTop + 0.5f * pH;
                    float cyFB = (float) fbH - cyTop;
                    float rectX = cx - 0.5f * pW;
                    float rectY = cyFB - 0.5f * pH;
                    b.putVec4(rectX, rectY, pW, pH);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    var w = widgets.get(i);
                    float widthPx = Math.max(0f, (w.x2() - w.x1()) * scale);
                    float heightPx = Math.max(0f, (w.y2() - w.y1()) * scale);
                    float maxRadius = Math.max(0f, 0.5f * Math.min(widthPx, heightPx) - 0.5f);
                    float rad = Math.max(0f, Math.min(w.cornerRadius() * scale, maxRadius));
                    b.putVec4(rad, rad, rad, rad);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    var style = widgets.get(i).style();
                    int c = style.getTintColor();
                    b.putVec4(ARGB.red(c) / 255f, ARGB.green(c) / 255f, ARGB.blue(c) / 255f, style.getTintAlpha());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    WidgetStyle s = widgets.get(i).style();
                    b.putVec4(s.getRefThickness(), s.getRefFactor(), s.getRefDispersion(), s.getRefFresnelRange());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }
            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    WidgetStyle s = widgets.get(i).style();
                    b.putVec4(s.getRefFresnelHardness(), s.getRefFresnelFactor(), s.getGlareRange(), s.getGlareHardness());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }
            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    WidgetStyle s = widgets.get(i).style();
                    b.putVec4(s.getGlareConvergence(), s.getGlareOppositeFactor(), s.getGlareFactor(), s.getGlareAngleRad());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    WidgetStyle s = widgets.get(i).style();
                    b.putVec4(s.getSmoothing(), 0f, 0f, 0f);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    var w = widgets.get(i);
                    ScreenRectangle sc = w.scissorArea();
                    if (sc != null) {
                        float sL = sc.left() * scale;
                        float sR = sc.right() * scale;
                        float sT = sc.top() * scale;
                        float sB = sc.bottom() * scale;
                        b.putVec4(sL, fbH - sB, sR, fbH - sT);
                    } else b.putVec4(0f, 0f, (float) mc.getMainRenderTarget().width, (float) mc.getMainRenderTarget().height);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    WidgetStyle s = widgets.get(i).style();
                    float sx = s.getShadowOffsetX() * scale;
                    float sy = s.getShadowOffsetY() * scale;
                    b.putVec4(s.getShadowExpand(), s.getShadowFactor(), sx, sy);
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    WidgetStyle s = widgets.get(i).style();
                    int col = s.getShadowColor();
                    b.putVec4(ARGB.red(col) / 255f, ARGB.green(col) / 255f, ARGB.blue(col) / 255f, s.getShadowColorAlpha());
                } else b.putVec4(0f, 0f, 0f, 0f);
            }

            for (int i = 0; i < MAX_WIDGETS; i++) {
                if (i < widgets.size()) {
                    WidgetStyle s = widgets.get(i).style();
                    int radius = Math.max(0, s.getBlurRadius());
                    Integer idx = blurRadiusToIndex.get(radius);
                    if (idx == null) idx = 0;
                    var w = widgets.get(i);
                    long key = rectKey(w.x1(), w.y1(), w.x2(), w.y2());
                    FadeState fs = fades.computeIfAbsent(key, k -> new FadeState());
                    fs.hover = smoothToward(fs.hover, Math.max(0f, Math.min(1f, w.hover())), dtSeconds, 0.12f);
                    fs.focus = smoothToward(fs.focus, Math.max(0f, Math.min(1f, w.focus())), dtSeconds, 0.18f);
                    b.putVec4((float) idx, fs.hover, fs.focus, (float) Math.atan2(w.pose().m01, w.pose().m00));
                } else b.putVec4(0f, 0f, 0f, 0f);
            }
        }
    }

    public int getCount() { return widgets.size(); }
    public GpuBuffer getSamplerInfoBuffer() { return samplerInfo; }
    public GpuBuffer getCustomUniformsBuffer() { return customUniforms; }
    public GpuBuffer getWidgetInfoBuffer() { return widgetInfo; }
    public GpuBuffer getBgConfigBuffer() { return bgConfig; }
    public GpuBuffer getProjectionBuffer() { return projectionBuffer; }
    public List<Integer> getUsedBlurRadiiOrdered() { return usedBlurRadiiOrdered; }
}
