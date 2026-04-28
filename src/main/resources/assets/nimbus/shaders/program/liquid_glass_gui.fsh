#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform sampler2D Sampler4;
uniform sampler2D Sampler5;

layout(std140) uniform Projection {
    mat4 ProjMat;
};

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
};

#define MAX_WIDGETS 64
layout(std140) uniform WidgetInfo {
    float Count;
    vec4 Rects[MAX_WIDGETS];
    vec4 Rads[MAX_WIDGETS];
    vec4 Tints[MAX_WIDGETS];
    vec4 Optics0[MAX_WIDGETS]; // [refraction, blur, chromatic, liquid_factor]
    vec4 Optics1[MAX_WIDGETS];
    vec4 Optics2[MAX_WIDGETS];
    vec4 Smoothings[MAX_WIDGETS];
    vec4 ScissorRects[MAX_WIDGETS];
    vec4 Shadow0[MAX_WIDGETS];
    vec4 ShadowColor[MAX_WIDGETS];
    vec4 Extra0[MAX_WIDGETS]; // [blur_index, layer_id, reserved, reserved]
};

out vec4 fragColor;

// Smooth minimum for liquid/metaball effects
float smin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * h * k * (1.0 / 6.0);
}

float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 inSize = InSize;
    if (inSize.x <= 0.0 || inSize.y <= 0.0) inSize = vec2(textureSize(Sampler1, 0));

    vec2 coord = gl_FragCoord.xy;
    vec2 uv = coord / inSize;
    vec3 finalBg = texture(Sampler1, uv).rgb;
    
    int nCount = int(Count + 0.5);
    float minDist = 1e6;
    int hitIdx = -1;
    
    // Metaball / Liquid accumulation
    float liquidDist = 1e6;
    vec4 liquidColor = vec4(0.0);
    float liquidAlpha = 0.0;

    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= nCount) break;
        
        vec4 sc = ScissorRects[i];
        if (coord.x < sc.x - 0.5 || coord.y < sc.y - 0.5 || coord.x > sc.z + 0.5 || coord.y > sc.w + 0.5) continue;
        
        vec4 rc = Rects[i];
        vec2 b = 0.5 * vec2(rc.z, rc.w);
        vec2 center = vec2(rc.x + b.x, rc.y + b.y);
        float d = sdRoundBox(coord - center, b, Rads[i]);
        
        float k = Optics0[i].w; // Liquid factor (smoothing)
        
        if (k > 0.01) {
            // Liquid path
            float weight = smoothstep(1.0, -1.0, d / k);
            liquidDist = smin(liquidDist, d, k);
            liquidColor = mix(liquidColor, Tints[i], weight);
            liquidAlpha = max(liquidAlpha, weight);
        } else {
            // Standard path
            if (d < minDist) {
                minDist = d;
                hitIdx = i;
            }
        }
    }

    // Render Liquid/Metaball part
    if (liquidDist < 5.0) {
        float alpha = smoothstep(1.0, -1.0, liquidDist);
        if (alpha > 0.0) {
            vec3 disp = texture(Sampler1, uv).rgb; // Simplification for liquid
            vec3 panelCol = mix(disp, liquidColor.rgb, liquidColor.a * 0.5);
            float rim = smoothstep(-2.0, 0.0, -liquidDist);
            panelCol += vec3(0.7, 0.9, 1.0) * rim * 0.1;
            finalBg = mix(finalBg, panelCol, alpha);
        }
    }

    // Render Standard part
    if (hitIdx != -1) {
        float alpha = smoothstep(1.0, -1.0, minDist);
        if (alpha > 0.0) {
            vec4 tint = Tints[hitIdx];
            vec4 rc = Rects[hitIdx];
            vec2 center = vec2(rc.x + 0.5*rc.z, rc.y + 0.5*rc.w);
            vec2 localP = coord - center;
            vec2 bSize = 0.5 * vec2(rc.z, rc.w);
            
            vec2 normP = localP / max(bSize, 1e-6);
            float distCenter = length(normP);
            float refrIntensity = smoothstep(0.2, 1.0, distCenter); 
            vec2 normal = normalize(localP); 
            
            vec2 refr = -normal * (45.0 * refrIntensity) / inSize.x;
            int bi = int(Extra0[hitIdx].x + 0.5);
            
            vec3 disp = texture(Sampler1, uv + refr).rgb;
            if (bi == 1) disp = texture(Sampler2, uv + refr).rgb;
            if (bi == 2) disp = texture(Sampler3, uv + refr).rgb;
            if (bi == 3) disp = texture(Sampler4, uv + refr).rgb;
            if (bi >= 4) disp = texture(Sampler5, uv + refr).rgb;

            vec3 panelCol = mix(disp, tint.rgb, tint.a * 0.5);
            float rim = smoothstep(-1.5, 0.0, -minDist);
            panelCol += vec3(0.5, 0.8, 1.0) * rim * 0.08;
            
            finalBg = mix(finalBg, panelCol, alpha);
        }
    }

    fragColor = vec4(finalBg, 1.0);
}
