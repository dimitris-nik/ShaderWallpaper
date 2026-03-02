#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;
uniform float speed;   // Control rotation speed
uniform float scale;   // Control Zoom/Camera distance
uniform float density; // Control Iteration count

#define PI 3.14159265359

// --------------------------------------------------------
// HG_SDF
// --------------------------------------------------------

void pR(inout vec2 p, float a) {
    p = cos(a)*p + sin(a)*vec2(p.y, -p.x);
}

float smax(float a, float b, float r) {
    vec2 u = max(vec2(r + a,r + b), vec2(0.0));
    return min(-r, max (a, b)) + length(u);
}

// --------------------------------------------------------
// Spectrum colour palette
// --------------------------------------------------------

vec3 pal( in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d ) {
    return a + b*cos( 6.28318*(c*t+d) );
}

vec3 spectrum(float n) {
    return pal( n, vec3(0.5,0.5,0.5),vec3(0.5,0.5,0.5),vec3(1.0,1.0,1.0),vec3(0.0,0.33,0.67) );
}

// --------------------------------------------------------
// Main SDF
// --------------------------------------------------------

vec4 inverseStereographic(vec3 p, out float k) {
    k = 2.0/(1.0+dot(p,p));
    return vec4(k*p,k-1.0);
}

float fTorus(vec4 p4) {
    float d1 = length(p4.xy) / length(p4.zw) - 1.0;
    float d2 = length(p4.zw) / length(p4.xy) - 1.0;
    float d = d1 < 0.0 ? -d1 : d2;
    d /= PI;
    return d;
}

float fixDistance(float d, float k) {
    float sn = sign(d);
    d = abs(d);
    d = d / k * 1.82;
    d += 1.0;
    d = pow(d, 0.5);
    d -= 1.0;
    d *= 5.0/3.0;
    d *= sn;
    return d;
}

float map(vec3 p, float t) {
    float k;
    vec4 p4 = inverseStereographic(p,k);

    pR(p4.zy, t * -PI / 2.0);
    pR(p4.xw, t * -PI / 2.0);

    // A thick walled clifford torus intersected with a sphere
    float d = fTorus(p4);
    d = abs(d);
    d -= 0.2;
    d = fixDistance(d, k);
    d = smax(d, length(p) - 1.85, 0.2);

    return d;
}

// --------------------------------------------------------
// Rendering
// --------------------------------------------------------

mat3 calcLookAtMatrix(vec3 ro, vec3 ta, vec3 up) {
    vec3 ww = normalize(ta - ro);
    vec3 uu = normalize(cross(ww,up));
    vec3 vv = normalize(cross(uu,ww));
    return mat3(uu, vv, ww);
}

void main() {
    float t = mod((time * speed) / 2.0, 1.0); // Integrated 'speed' param

    // Integrated 'scale' param (multiplies original constant)
    vec3 camPos = vec3(1.8, 5.5, -5.5) * scale;

    vec3 camTar = vec3(0.0, 0.0, 0.0);
    vec3 camUp = vec3(-1.0, 0.0, -1.5);
    mat3 camMat = calcLookAtMatrix(camPos, camTar, camUp);
    float focalLength = 5.0;

    // UV calc for GLSL ES 2.0
    vec2 p = (-resolution.xy + 2.0 * gl_FragCoord.xy) / resolution.y;

    vec3 rayDirection = normalize(camMat * vec3(p, focalLength));
    vec3 rayPosition = camPos;
    float rayLength = 0.0;

    float distance = 0.0;
    vec3 color = vec3(0.0);

    vec3 c;

    // Integrated 'density' param to control loop count
    // NOTE: In GLES 2.0 loops must usually present constant bounds.
    // However, if we break based on uniform, we must use a constant high max.
    // Here we use a fixed 120 loop but break early based on density.
    float FUDGE_FACTORR = 0.8;
    float INTERSECTION_PRECISION = 0.001;
    // Increased MAX_DIST to allow zooming out further without fading too much
    float MAX_DIST = 35.0;

    for (int i = 0; i < 150; i++) {
        // Break if we exceed user defined density
        if (float(i) > density) break;

        // Step a little slower so we can accumulate glow
        rayLength += max(INTERSECTION_PRECISION, abs(distance) * FUDGE_FACTORR);
        rayPosition = camPos + rayDirection * rayLength;
        distance = map(rayPosition, t);

        // Add a lot of light when we're really close to the surface
        c = vec3(max(0.0, 0.01 - abs(distance)) * 0.5);
        c *= vec3(1.4, 2.1, 1.7); // blue green tint

        // Accumulate some purple glow for every step
        c += vec3(0.6, 0.25, 0.7) * FUDGE_FACTORR / 160.0;

        // Adjusted smoothstep range to account for larger MAX_DIST/Scale
        c *= smoothstep(MAX_DIST, 7.0, length(rayPosition));

        // Fade out further away from the camera
        float rl = smoothstep(MAX_DIST, 0.1, rayLength);
        c *= rl;

        // Vary colour as we move through space
        c *= spectrum(rl * 6.0 - 0.6);

        color += c;

        if (rayLength > MAX_DIST) {
            break;
        }
    }

    // Tonemapping and gamma
    color = pow(color, vec3(1.0 / 1.8)) * 2.0;
    color = pow(color, vec3(2.0)) * 3.0;
    color = pow(color, vec3(1.0 / 2.2));

    gl_FragColor = vec4(color, 1.0);
}

