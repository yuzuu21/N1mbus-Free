#version 150
uniform sampler2D Sampler0;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

#define MAX_WIDGETS 128
layout(std140) uniform RoundedInfo {
    float Count;
    vec4 Rects[MAX_WIDGETS];
    vec4 Colors[MAX_WIDGETS];
    vec4 Params[MAX_WIDGETS];
    vec4 Scissors[MAX_WIDGETS];
};

out vec4 fragColor;

float sdRoundRect(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    vec2 uv = gl_FragCoord.xy / InSize;
    vec4 outColor = texture(Sampler0, uv);

    int n = int(Count + 0.5);
    for (int i = 0; i < MAX_WIDGETS; i++) {
        if (i >= n) break;

        vec4 sc = Scissors[i];
        if (gl_FragCoord.x < sc.x - 0.5 || gl_FragCoord.y < sc.y - 0.5 || gl_FragCoord.x > sc.z + 0.5 || gl_FragCoord.y > sc.w + 0.5) continue;

        vec4 rect = Rects[i];
        vec4 color = Colors[i];
        vec4 params = Params[i]; // x: radius, y: aa
        if (color.a <= 0.0) continue;

        vec2 center = vec2(rect.x + rect.z * 0.5, rect.y + rect.w * 0.5);
        vec2 halfSize = max(vec2(0.0), vec2(rect.z, rect.w) * 0.5);
        float radius = min(params.x, max(0.0, min(halfSize.x, halfSize.y) - 0.75));
        vec2 p = gl_FragCoord.xy - center + vec2(0.5);
        float d = sdRoundRect(p, halfSize, radius);
        float aa = max(0.85, fwidth(d) * 1.25 + params.y * 0.05);
        float mask = 1.0 - smoothstep(-aa, aa, d);

        outColor.rgb = mix(outColor.rgb, color.rgb, color.a * mask);
    }

    outColor.a = 1.0;
    fragColor = outColor;
}
