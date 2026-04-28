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

void main() {
    vec2 inSize = InSize;
    if (inSize.x <= 0.0 || inSize.y <= 0.0) inSize = textureSize(Sampler0, 0);

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
            vec3 disp = texture(Sampler1, uv).rgb;
            vec4 rc = Rects[hitIdx];
            
            // HTML Based: #0a0a0c with 0.03 gloss
            vec3 panelCol = mix(disp, vec3(0.039), 0.7); 
            panelCol = mix(panelCol, tint.rgb, tint.a * 0.15);
            
            // Multi-shadow glow emulation from HTML
            float absD = abs(minDist);
            float core = exp(-absD * 1.5) * 1.5;
            float glow = exp(-absD * 0.2) * 0.8;
            panelCol += tint.rgb * (core + glow);
            
            // Neon Line (Animated Flow)
            float edgeY = rc.y + rc.w - 1.5;
            if (abs(coord.y - edgeY) < 1.0) {
                float flow = fract(coord.x * 0.004 - Time * 0.6);
                float lineGlow = smoothstep(0.15, 0.0, abs(flow - 0.5)) * 2.5;
                panelCol += vec3(1.0, 0.9, 0.3) * lineGlow;
            }

            finalBg = mix(finalBg, panelCol, alpha);
        }
    }

    fragColor = vec4(finalBg, 1.0);
}
