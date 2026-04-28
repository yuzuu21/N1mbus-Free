#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

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

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
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
    vec3 finalBg = texture(Sampler0, uv).rgb;
    
    int nCount = int(Count + 0.5);
    float minDist = 1e6;
    int hitIdx = -1;

    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= nCount) break;
        vec4 sc = ScissorRects[i];
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
        float alpha = smoothstep(1.0, -1.0, minDist);
        if (alpha > 0.0) {
            vec4 tint = Tints[hitIdx];
            
            // --- Hologram Glitch Logic ---
            float gTime = Time * 2.0;
            float glitchVal = step(0.97, noise(vec2(floor(gTime * 5.0), 0.0)));
            float hShift = glitchVal * (noise(vec2(0.0, floor(coord.y * 0.1))) - 0.5) * 0.05;
            
            // Chromatic Aberration (RGB Split)
            vec2 offset = vec2(hShift, 0.0);
            float r = texture(Sampler1, uv + offset).r;
            float g = texture(Sampler1, uv).g;
            float b = texture(Sampler1, uv - offset).b;
            vec3 holoBg = vec3(r, g, b);
            
            // Adding Cyan/Blue Tint
            holoBg = mix(holoBg, vec3(0.0, 0.8, 1.0), 0.2);
            
            // Scanlines
            float scan = sin(coord.y * 1.5 + Time * 20.0) * 0.04;
            holoBg += scan;
            
            // Flicker
            float flicker = 0.95 + 0.05 * sin(Time * 100.0);
            holoBg *= flicker;
            
            // Static / Noise
            float n = (noise(coord + Time) - 0.5) * 0.08;
            holoBg += n;

            vec3 panelCol = mix(holoBg, tint.rgb, tint.a * 0.3);
            
            // Glowing border
            float glow = exp(-3.0 * abs(minDist / 5.0));
            panelCol += vec3(0.0, 0.7, 1.0) * glow * 0.8;
            
            finalBg = mix(finalBg, panelCol, alpha);
        }
    }

    fragColor = vec4(finalBg, 1.0);
}
