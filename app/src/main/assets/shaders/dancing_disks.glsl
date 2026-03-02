#ifdef GL_ES
// Enable standard derivatives to calculate exact pixel widths for anti-aliasing
#extension GL_OES_standard_derivatives : enable
precision highp float;
#endif

#define PI 3.14159265359

uniform float time;
uniform vec2 resolution;
uniform float speed_val;
uniform float scale;
uniform vec3 col1;
uniform vec3 col2;
uniform vec3 col3;

float disk(vec2 r, vec2 center, float radius) {
float dist = length(r - center);
// fwidth gives us the exact change in distance between adjacent pixels.
// This ensures a perfectly crisp, 1-pixel anti-aliased edge regardless of distortion.
float fw = fwidth(dist);
return 1.0 - smoothstep(radius - fw, radius + fw, dist);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    float t = time * 2.0;
    vec2 r = (2.0 * fragCoord.xy - resolution.xy) / resolution.y;

    // Space distortion
    r *= 1.0 + 0.05 * sin(r.x * 5.0 + time) + 0.05 * sin(r.y * 3.0 + time);
    r *= 1.0 + 0.2 * length(r);
    r *= scale;

    float side = 0.5;
    vec2 r2 = mod(r, side);
    vec2 r3 = r2 - side / 2.0;

    float i = floor(r.x / side) + 2.0;
    float j = floor(r.y / side) + 4.0;
    float ii = r.x / side + 2.0;
    float jj = r.y / side + 4.0;

    vec3 pix = vec3(1.0);
    float rad;
    float disks;

    // Disk 1
    rad = 0.15 + 0.05 * sin(t + ii * jj);
    disks = disk(r3, vec2(0.0, 0.0), rad);
    pix = mix(pix, col2, disks);

    // Disk 2 (Moving)
    float speed = speed_val * 2.0;
    float tt = time * speed + 0.1 * i + 0.08 * j;
    float stopEveryAngle = PI / 2.0;
    float stopRatio = 0.7;
    float t1 = (floor(tt) + smoothstep(0.0, 1.0 - stopRatio, fract(tt))) * stopEveryAngle;

    float x = -0.07 * cos(t1 + i);
    // Tweaked max Y multiplier from 0.055 to 0.04 to stop it from clipping the cell boundary
    float y = 0.04 * (sin(t1 + j) + cos(t1 + i));
    rad = 0.1 + 0.05 * sin(t + i + j);
    disks = disk(r3, vec2(x, y), rad);
    pix = mix(pix, col1, disks);

    // Disk 3 (Outer)
    // Tweaked base radius from 0.2 to 0.19 to stop the blurred edge from clipping
    rad = 0.19 + 0.05 * sin(t * (1.0 + 0.01 * i));
    disks = disk(r3, vec2(0.0, 0.0), rad);
    pix += 0.2 * col3 * disks * sin(t + i * j + i);

    fragColor = vec4(pix, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}