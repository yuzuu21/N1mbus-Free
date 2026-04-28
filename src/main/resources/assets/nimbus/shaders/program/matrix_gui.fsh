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

// Ultra Smooth Matrix Digital Rain
float matrix(vec2 p, float t, vec3 color) {
    float grid_x = floor(p.x * 32.0);
    float offset = noise(vec2(grid_x, 1.23)) * 100.0;
    float speed = 0.3 + noise(vec2(grid_x, 4.56)) * 0.4;
    
    // Smooth trail calculation
    float y = fract(p.y + t * speed + offset);
    float glow = pow(y, 8.0) * 0.7; // Long smooth trail
    float head = smoothstep(0.95, 1.0, y) * 1.5; // Bright leading edge
    
    // Random character density (Smooth temporal noise)
    float char_row = floor(p.y * 50.0 + t * 0.1);
    float char_noise = noise(vec2(grid_x, char_row));
    float mask = step(0.4, char_noise);
    
    return (head + glow) * mask;
}

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
            
            // Subtle Matrix Green tint background
            vec3 panelCol = mix(disp, vec3(0.01, 0.05, 0.02), 0.5);
            panelCol = mix(panelCol, tint.rgb, tint.a * 0.3);
            
            // Ultra-smooth digital rain
            float m = matrix(uv, Time * 0.8, tint.rgb);
            panelCol += tint.rgb * m * 0.6;
            
            // Rim highlighting
            float rim = smoothstep(-1.5, 0.0, -minDist);
            panelCol += tint.rgb * rim * 0.3;
            
            finalBg = mix(finalBg, panelCol, alpha);
        }
    }

    fragColor = vec4(finalBg, 1.0);
}
