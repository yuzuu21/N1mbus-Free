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

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// Elegant Sakura Petal physics (Slower and more fluttery)
vec3 getSakura(vec2 p, float t, int id) {
    float n = noise(vec2(float(id), 123.45));
    // Slower horizontal sway
    float x = sin(t * 0.45 + n * 20.0) * 0.15;
    // Much slower falling speed (0.1 multiplier)
    float y = fract(t * 0.08 + n) * 1.5 - 0.2;
    vec2 pos = vec2(n * 2.0 - 1.0 + x, 0.7 - y);
    
    // Rotating petal shape
    float angle = t * 0.5 + n * 6.28;
    mat2 rot = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    vec2 localP = (p - pos) * rot;
    
    // Heart-like petal shape
    float d = length(localP * vec2(1.0, 1.4 + 0.3 * sin(t + n)));
    float petal = smoothstep(0.012, 0.0, d);
    return vec3(1.0, 0.75, 0.85) * petal * (0.7 + 0.3 * n);
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
    vec2 pCentered = (coord - 0.5 * inSize) / inSize.y;
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
            
            vec3 panelCol = mix(disp, vec3(0.15, 0.05, 0.08), 0.3);
            panelCol = mix(panelCol, tint.rgb, tint.a * 0.45);
            
            // Falling Sakura Petals particles (Slow & Elegant)
            vec3 petals = vec3(0.0);
            for(int j=0; j<15; j++) {
                petals += getSakura(pCentered * 1.6, Time * 1.1, j + hitIdx * 11);
            }
            panelCol += petals * smoothstep(2.0, -2.0, minDist);
            
            float glow = exp(-4.0 * abs(minDist / 12.0));
            panelCol += vec3(1.0, 0.7, 0.85) * glow * 0.35;
            
            finalBg = mix(finalBg, panelCol, alpha);
        }
    }

    fragColor = vec4(finalBg, 1.0);
}
