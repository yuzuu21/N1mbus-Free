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

float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float grid(vec2 p, float size) {
    vec2 g = abs(fract(p * size - 0.5) - 0.5) / fwidth(p * size);
    return 1.0 - clamp(min(g.x, g.y), 0.0, 1.0);
}

void main() {
    vec2 inSize = InSize;
    if (inSize.x <= 0.0 || inSize.y <= 0.0) inSize = textureSize(Sampler0, 0);

    vec2 coord = gl_FragCoord.xy;
    vec2 uv = coord / inSize;
    vec3 base = texture(Sampler0, uv).rgb;

    // Background Cyber Grid (Subtle)
    vec2 pNorm = (coord - 0.5 * inSize) / inSize.y;
    float z = 1.0 / (0.6 - pNorm.y * 0.4);
    vec2 uvGrid = vec2(pNorm.x * z * 0.8, z + Time * 0.4);
    float g = grid(uvGrid, 6.0) * smoothstep(0.0, 0.5, 1.0/z);
    vec3 gridCol = vec3(0.0, 0.5, 1.0) * g * 0.4; // Softer grid

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
            vec3 disp = texture(Sampler1, uv).rgb;
            
            // Neon Matrix Panel Style
            vec3 panelCol = mix(disp * 0.6, tint.rgb, tint.a * 0.4);
            
            // Interior micro-grid
            float ig = grid(coord, 0.1) * 0.05;
            panelCol += vec3(0.0, 0.8, 1.0) * ig;

            // Chromatic rim
            float rim = smoothstep(-2.0, 0.0, -minDist);
            panelCol += vec3(0.0, 0.6, 1.0) * rim * 0.5;

            fragColor = vec4(mix(base + gridCol, panelCol, alpha), 1.0);
            return;
        }
    }

    fragColor = vec4(base + gridCol, 1.0);
}
