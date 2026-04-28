package n1mbus.ghs.client.api;

import n1mbus.ghs.client.runtime.N1mbusGhsAnim;

public class WidgetStyle {

    private boolean hasTint;
    private int tintColor;
    private float tintAlpha;

    private boolean hasSmoothing;
    private float smoothingFactor;

    private boolean hasBlurRadius;
    private int blurRadius;

    private boolean hasShadow;
    private float shadowExpand;
    private float shadowFactor;
    private float shadowOffsetX;
    private float shadowOffsetY;
    private int shadowColor;
    private float shadowColorAlpha;

    private boolean hasRefraction;
    private float refThickness;
    private float refFactor;
    private float refDispersion;
    private float refFresnelRange;
    private float refFresnelHardness;
    private float refFresnelFactor;

    private boolean hasGlare;
    private float glareRange;
    private float glareHardness;
    private float glareConvergence;
    private float glareOppositeFactor;
    private float glareFactor;
    private float glareAngleRad;

    public static WidgetStyle create() { return new WidgetStyle(); }

    public WidgetStyle tint(int color, float alpha) { this.hasTint = true; this.tintColor = color; this.tintAlpha = alpha; return this; }
    public WidgetStyle smoothing(float factor) { this.hasSmoothing = true; this.smoothingFactor = factor; return this; }
    public WidgetStyle blurRadius(int radius) { this.hasBlurRadius = true; this.blurRadius = Math.max(0, radius); return this; }
    public WidgetStyle shadow(float expand, float factor, float offsetX, float offsetY) { this.hasShadow = true; this.shadowExpand = expand; this.shadowFactor = factor; this.shadowOffsetX = offsetX; this.shadowOffsetY = offsetY; return this; }
    public WidgetStyle shadowColor(int color, float alpha) { this.hasShadow = true; this.shadowColor = color; this.shadowColorAlpha = alpha; return this; }
    public WidgetStyle refractionThickness(float v) { this.hasRefraction = true; this.refThickness = v; return this; }
    public WidgetStyle refractionFactor(float v) { this.hasRefraction = true; this.refFactor = v; return this; }
    public WidgetStyle refractionDispersion(float v) { this.hasRefraction = true; this.refDispersion = v; return this; }
    public WidgetStyle fresnelRange(float v) { this.hasRefraction = true; this.refFresnelRange = v; return this; }
    public WidgetStyle fresnelHardness(float v) { this.hasRefraction = true; this.refFresnelHardness = v; return this; }
    public WidgetStyle fresnelFactor(float v) { this.hasRefraction = true; this.refFresnelFactor = v; return this; }
    public WidgetStyle glareRange(float v) { this.hasGlare = true; this.glareRange = v; return this; }
    public WidgetStyle glareHardness(float v) { this.hasGlare = true; this.glareHardness = v; return this; }
    public WidgetStyle glareConvergence(float v) { this.hasGlare = true; this.glareConvergence = v; return this; }
    public WidgetStyle glareOppositeFactor(float v) { this.hasGlare = true; this.glareOppositeFactor = v; return this; }
    public WidgetStyle glareFactor(float v) { this.hasGlare = true; this.glareFactor = v; return this; }
    public WidgetStyle glareAngleRad(float v) { this.hasGlare = true; this.glareAngleRad = v; return this; }

    public int getTintColor() { return hasTint ? tintColor : N1mbusGhsConfig.INSTANCE.defaultTintColor; }
    public float getTintAlpha() { return hasTint ? tintAlpha : N1mbusGhsAnim.INSTANCE.tintAlpha(); }

    public float getSmoothing() { return hasSmoothing ? smoothingFactor : N1mbusGhsAnim.INSTANCE.smoothing(); }
    public int getBlurRadius() { return hasBlurRadius ? blurRadius : N1mbusGhsAnim.INSTANCE.blurRadiusInt(); }

    public float getShadowExpand() { return hasShadow ? shadowExpand : N1mbusGhsAnim.INSTANCE.shadowExpand(); }
    public float getShadowFactor() { return hasShadow ? shadowFactor : N1mbusGhsAnim.INSTANCE.shadowFactor(); }
    public float getShadowOffsetX() { return hasShadow ? shadowOffsetX : N1mbusGhsAnim.INSTANCE.shadowOffsetX(); }
    public float getShadowOffsetY() { return hasShadow ? shadowOffsetY : N1mbusGhsAnim.INSTANCE.shadowOffsetY(); }
    public int getShadowColor() { return hasShadow ? shadowColor : N1mbusGhsConfig.INSTANCE.defaultShadowColor; }
    public float getShadowColorAlpha() { return hasShadow ? shadowColorAlpha : N1mbusGhsConfig.INSTANCE.defaultShadowColorAlpha; }

    public float getRefThickness() { return hasRefraction ? refThickness : N1mbusGhsAnim.INSTANCE.refThickness(); }
    public float getRefFactor() { return hasRefraction ? refFactor : N1mbusGhsAnim.INSTANCE.refFactor(); }
    public float getRefDispersion() { return hasRefraction ? refDispersion : N1mbusGhsAnim.INSTANCE.refDispersion(); }
    public float getRefFresnelRange() { return hasRefraction ? refFresnelRange : N1mbusGhsAnim.INSTANCE.refFresnelRange(); }
    public float getRefFresnelHardness() { return hasRefraction ? refFresnelHardness : N1mbusGhsAnim.INSTANCE.refFresnelHardness(); }
    public float getRefFresnelFactor() { return hasRefraction ? refFresnelFactor : N1mbusGhsAnim.INSTANCE.refFresnelFactor(); }

    public float getGlareRange() { return hasGlare ? glareRange : N1mbusGhsAnim.INSTANCE.glareRange(); }
    public float getGlareHardness() { return hasGlare ? glareHardness : N1mbusGhsAnim.INSTANCE.glareHardness(); }
    public float getGlareConvergence() { return hasGlare ? glareConvergence : N1mbusGhsAnim.INSTANCE.glareConvergence(); }
    public float getGlareOppositeFactor() { return hasGlare ? glareOppositeFactor : N1mbusGhsAnim.INSTANCE.glareOppositeFactor(); }
    public float getGlareFactor() { return hasGlare ? glareFactor : N1mbusGhsAnim.INSTANCE.glareFactor(); }
    public float getGlareAngleRad() { return hasGlare ? glareAngleRad : N1mbusGhsAnim.INSTANCE.glareAngleRad(); }
}