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
            vec4 rc = Rects[hitIdx];
            vec2 center = vec2(rc.x + 0.5*rc.z, rc.y + 0.5*rc.w);
            vec2 normal = normalize(coord - center); 
            
            // Heavy refractive distortion (Liquid Metal style)
            vec2 refr = -normal * 120.0 / inSize.x;
            vec3 disp = texture(Sampler1, uv + refr).rgb;
            
            // Specular / Reflection highlight
            vec3 lightDir = normalize(vec3(cos(Time), 0.5, sin(Time)));
            float spec = pow(max(dot(vec3(normal, 1.0), lightDir), 0.0), 32.0);
            
            // Fresnel / Rim
            float fresnel = pow(1.0 - max(dot(vec3(normal, 0.0), vec3(0,0,1)), 0.0), 3.0);
            
            vec3 panelCol = mix(disp, vec3(0.8, 0.82, 0.85), 0.4); // Silver base
            panelCol += vec3(0.9, 0.95, 1.0) * spec * 0.7; // Specular shine
            panelCol += vec3(0.7, 0.8, 1.0) * fresnel * 0.4; // Rim light
            
            // Add some "flowing" oily sheen
            panelCol *= 0.9 + 0.1 * sin(coord.y * 0.05 + Time * 2.0);

            finalBg = mix(finalBg, panelCol, alpha);
        }
    }

    fragColor = vec4(finalBg, 1.0);
}
