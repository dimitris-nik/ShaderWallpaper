#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;
uniform float speed;      // User controlled speed
uniform float intensity;  // User controlled intensity (was 0.005)

#define TAU 6.28318530718
#define MAX_ITER 5

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // Apply user speed. Original was iTime * .5
    float t_val = time * speed + 23.0;

    vec2 uv = fragCoord.xy / resolution.xy;

    // Correct aspect ratio
    uv.x *= resolution.x / resolution.y;

    // Tiling logic from original
    vec2 p = mod(uv*TAU, TAU)-250.0;

    vec2 i = vec2(p);
    float c = 1.0;
    float inten = .005; // Default fallback
    if (intensity > 0.0) inten = intensity;

    for (int n = 0; n < MAX_ITER; n++)
    {
        float t = t_val * (1.0 - (3.5 / float(n+1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        c += 1.0/length(vec2(p.x / (sin(i.x+t)/inten),p.y / (cos(i.y+t)/inten)));
    }

    c /= float(MAX_ITER);
    c = 1.17-pow(c, 1.4);

    vec3 colour = vec3(pow(abs(c), 8.0));

    // Original: clamp(colour + vec3(0.0, 0.35, 0.5), 0.0, 1.0)
    // Blue-ish tint
    colour = clamp(colour + vec3(0.0, 0.35, 0.5), 0.0, 1.0);

    fragColor = vec4(colour, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}


