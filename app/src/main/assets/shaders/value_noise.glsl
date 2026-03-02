#ifdef GL_ES
precision highp float;
#endif

// App-supplied uniforms
uniform float time;
uniform vec2 resolution;

uniform vec3 color1;
uniform vec3 color2;
uniform vec3 color3;
uniform vec3 color4;

// User-configurable uniforms
uniform float speed;
uniform float duration;

// A mobile-safe hash function
float hash_21(vec2 p)
{
// Safety check: keep coordinates small before dot products
p = mod(p, 289.0);
vec3 p3 = fract(vec3(p.xyx) * 0.1031);
p3 += dot(p3, p3.yzx + 33.33);
return fract((p3.x + p3.y) * p3.z);
}

float rand1(float p)
{
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float value_noise(vec2 p)
{
vec2 i = floor(p);
vec2 f = fract(p);

// QUINTIC INTERPOLATION:
// This entirely removes the C2 derivative discontinuities
// that cause grid-snapping when domain warping is applied.
f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

// Explicitly wrap integer coordinates to prevent precision loss
float a = hash_21(mod(i, 289.0));
float b = hash_21(mod(i + vec2(1.0, 0.0), 289.0));
float c = hash_21(mod(i + vec2(0.0, 1.0), 289.0));
float d = hash_21(mod(i + vec2(1.0, 1.0), 289.0));

return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float stepsmooth(float v, float a)
{
    return smoothstep(v, v + 0.1, a);
}

vec3 samplePoint(vec2 p) {
// Prevent float precision explosion over days of phone uptime
// by seamlessly looping time on a large interval
float t = mod(time * speed, duration * 1000.0);
float dur = duration;
vec2 op = p;

// Restrict the seed to a safe float range
float seed = mod(9000.0 * (rand1(floor(t/dur)) - 0.5), 289.0);

float v = value_noise(18.0 * p + seed);

// Domain warp
p += v * mod(t, dur);

// FBM
v = value_noise(5.0 * p + seed);

float d = max(abs(op.x) / (resolution.x / resolution.y), abs(op.y));
v *= 1.0 / (1.0 + pow(d, 3.0) * 5.0);

vec3 col = vec3(0.0);
col = mix(col, color1, stepsmooth(0.5, v));
col = mix(col, color2, stepsmooth(0.6, v));
col = mix(col, color3, stepsmooth(0.7, v));
col = mix(col, color4, stepsmooth(0.8, v));
col *= smoothstep(0.0, 2.0, min(mod(t, dur), 2.0)); // fade in

return col;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
    vec2 uv = (fragCoord - 0.5 * resolution.xy) / resolution.y;
    vec3 col = vec3(0.0);

    // Anti-aliasing
    vec2 px = 1.0 / resolution.xy;
    float c = 0.0;

    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            col += samplePoint(uv + px / 4.0 * vec2(float(i), float(j)));
            c += 1.0;
        }
    }
    col /= c;

    col = pow(col, vec3(0.4545));
    fragColor = vec4(col, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}