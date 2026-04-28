#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform sampler2D Sampler4;
uniform sampler2D Sampler5;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform CustomUniforms {
    float Time;
    vec4 Mouse;
    float ScreenWantsBlur;
    vec3 RIM_LIGHT_VEC;
    vec4 RIM_LIGHT_COLOR;
    float EPS_PIX;
    float DebugStep;
    float Pixelated;
    float PixelGridSize;
    float HoverScalePx;
    float FocusScalePx;
    float FocusBorderWidthPx;
    float FocusBorderIntensity;
    float FocusBorderSpeed;
};

#define MAX_WIDGETS 64
layout(std140) uniform WidgetInfo {
    float Count;
    vec4 Rects[MAX_WIDGETS];
    vec4 Rads[MAX_WIDGETS];
    vec4 Tints[MAX_WIDGETS];
    vec4 Optics0[MAX_WIDGETS];
    vec4 Optics1[MAX_WIDGETS];
    vec4 Optics2[MAX_WIDGETS];
    vec4 Smoothings[MAX_WIDGETS];
    vec4 ScissorRects[MAX_WIDGETS];
    vec4 Shadow0[MAX_WIDGETS];
    vec4 ShadowColor[MAX_WIDGETS];
    vec4 Extra0[MAX_WIDGETS];
};

out vec4 fragColor;

mat2 rot(float a) { float s=sin(a),c=cos(a); return mat2(c,-s,s,c); }

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float smoothNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f*f*(3.0-2.0*f);
    float a = noise(i);
    float b = noise(i + vec2(1.0, 0.0));
    float c = noise(i + vec2(0.0, 1.0));
    float d = noise(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 m = rot(0.5);
    for (int i = 0; i < 5; i++) {
        v += a * smoothNoise(p);
        p = m * p * 2.0;
        a *= 0.5;
    }
    return v;
}

vec3 getAurora(vec2 uv, float time) {
    vec3 col = vec3(0.0);
    vec2 p = uv * 2.0;
    float n = fbm(p + time * 0.1);
    float n2 = fbm(p - time * 0.08 + n * 0.5);
    float wave = smoothstep(0.3, 0.8, n2);
    vec3 c1 = vec3(0.0, 0.5, 0.9);
    vec3 c2 = vec3(0.1, 0.8, 0.4);
    vec3 c3 = vec3(0.7, 0.2, 0.8);
    col = mix(c1, c2, n);
    col = mix(col, c3, n2);
    return col * wave;
}

float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 inSize = InSize;
    if (inSize.x <= 0.0 || inSize.y <= 0.0) inSize = vec2(textureSize(Sampler0, 0));

    vec2 coord = gl_FragCoord.xy;
    vec2 uv = coord / inSize;
    vec2 uvCentered = (coord - 0.5 * inSize) / inSize.y;
    vec3 finalBg = texture(Sampler1, uv).rgb;

    int nCount = int(Count + 0.5);
    float minDist = 1e6;
    int hitIdx = -1;

    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= nCount) break;
        vec4 sc = ScissorRects[i];
        // Scissor with tiny margin to prevent "grazing"
        if (coord.x < sc.x - 0.5 || coord.y < sc.y - 0.5 || coord.x > sc.z + 0.5 || coord.y > sc.w + 0.5) continue;

        vec4 rc = Rects[i];
        vec2 b = 0.5 * vec2(rc.z, rc.w);
        vec2 center = vec2(rc.x + b.x, rc.y + b.y);
        
        float d = sdRoundBox(coord - center, b, Rads[i]);
        if (d < minDist) {
            minDist = d;
            hitIdx = i;
        }
    }

    if (hitIdx != -1) {
        // Anti-aliased boundary blending
        float alpha = smoothstep(1.0, -1.0, minDist);
        if (alpha > 0.0) {
            float inside = smoothstep(2.0, -2.5, minDist);
            vec4 tint = Tints[hitIdx];
            vec4 rc = Rects[hitIdx];
            vec2 center = vec2(rc.x + 0.5*rc.z, rc.y + 0.5*rc.w);
            
            vec2 localP = coord - center;
            vec2 bSize = 0.5 * vec2(rc.z, rc.w);
            vec2 normP = localP / max(bSize, 1e-6);
            vec2 normal = normalize(normP * abs(normP)); 

            // Fluid Refraction
            vec2 refr = -normal * (65.0 * inside) / inSize.x;
            refr += vec2(sin(Time * 0.45), cos(Time * 0.35)) * 0.0016 * inside;
            vec3 blurred = texture(Sampler1, uv + refr).rgb;
            
            // Floating Aurora
            vec2 driftUv = uvCentered * 2.1 + vec2(Time * 0.045, Time * 0.025);
            vec3 pAurora = getAurora(driftUv, Time * 0.45 + float(hitIdx) * 0.8);
            
            vec3 panelCol = mix(blurred, tint.rgb, tint.a * 0.38);
            panelCol = mix(panelCol, pAurora, 0.62 * inside); 
            
            // Glow & Edge
            float glow = exp(-3.8 * abs(minDist));
            panelCol += vec3(0.6, 0.9, 1.0) * glow * 0.75;
            panelCol += vec3(0.1, 0.15, 0.4) * inside;
            
            finalBg = mix(finalBg, panelCol, alpha);
        }
    }

    fragColor = vec4(finalBg, 1.0);
}
