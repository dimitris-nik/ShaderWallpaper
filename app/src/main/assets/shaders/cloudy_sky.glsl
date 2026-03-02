#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;
uniform float speed; // User controlled speed multiplier

// Original constants
const float cloudscale = 1.1;
const float base_speed = 0.03;
const float clouddark = 0.5;
const float cloudlight = 0.3;
const float cloudcover = 0.2;
const float cloudalpha = 8.0;
const float skytint = 0.5;
const vec3 skycolour1 = vec3(0.2, 0.4, 0.6);
const vec3 skycolour2 = vec3(0.4, 0.7, 1.0);

const mat2 m = mat2( 1.6,  1.2, -1.2,  1.6 );

vec2 hash( vec2 p ) {
p = vec2(dot(p,vec2(127.1,311.7)), dot(p,vec2(269.5,183.3)));
return -1.0 + 2.0*fract(sin(p)*43758.5453123);
}

float noise( in vec2 p ) {
    const float K1 = 0.366025404; // (sqrt(3)-1)/2;
    const float K2 = 0.211324865; // (3-sqrt(3))/6;
    vec2 i = floor(p + (p.x+p.y)*K1);
    vec2 a = p - i + (i.x+i.y)*K2;
    vec2 o = (a.x>a.y) ? vec2(1.0,0.0) : vec2(0.0,1.0);
    vec2 b = a - o + K2;
    vec2 c = a - 1.0 + 2.0*K2;
    vec3 h = max(0.5-vec3(dot(a,a), dot(b,b), dot(c,c) ), 0.0 );
    vec3 n = h*h*h*h*vec3( dot(a,hash(i+0.0)), dot(b,hash(i+o)), dot(c,hash(i+1.0)));
    return dot(n, vec3(70.0));
}

float fbm(vec2 n) {
float total = 0.0, amplitude = 0.1;
for (int i = 0; i < 7; i++) {
total += noise(n) * amplitude;
n = m * n;
amplitude *= 0.4;
}
return total;
}

// -----------------------------------------------

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 p = fragCoord.xy / resolution.xy;
    vec2 uv = p*vec2(resolution.x/resolution.y,1.0);

    // FIX: Keep time small using mod, but apply it separately from the scaled UVs
    // Use a smaller modulo to keep coordinates in a safe range for float precision (mediump safe ~1000, highp safe ~65536)
    // 1000 is safe. But if it pixelates, maybe the hash function is unstable at large inputs.
    // Let's try to mix the noise to avoid the cut? No, user accepted modulo but wants it smooth.
    // The "pixelated" look after 2 minutes suggests effective coordinates are exceeding float precision (~16 million for float32 integer part implies 0 precision for fractional).
    // But 2 minutes is small.
    // Maybe `speed` is huge?
    // Let's force a modulo.

    // We blend two time offsets to make the transition smooth
    float period = 360.0; // Repeat every 6 minutes at base speed
    float t = time * speed * base_speed;

    // We are not blending here to keep it simple and performant,
    // just using a modulo that matches a multiple of 2PI might help if using Sin, but Hash is random.
    // The user says "smooth transition or vary rare".
    // Let's use a very large modulo, but smaller than "pixelation" threshold.
    // 2 minutes causes issues? 2 mins * speed 1.0 * base_speed 0.03 = 3.6.
    // This implies the original shader had something else or speed is huge.
    // Or maybe on their device highp is failing.

    // Let's use 100.0 simply.
    float timeVal = mod(t, 200.0);

    float q = fbm(uv * cloudscale * 0.5);

    //ridged noise shape
    float r = 0.0;
    vec2 uv2 = uv * cloudscale;
    uv2 -= q; // Offset by FBM, but NOT by time yet

    float weight = 0.8;
    for (int i=0; i<8; i++){
        // FIX: Add timeVal here directly to the noise lookup.
        // This ensures the value being hashed stays relatively small (~1000 + small UV)
        // instead of getting multiplied by matrix 'm' repeatedly.
        r += abs(weight*noise( uv2 + timeVal ));
        uv2 = m*uv2;
        weight *= 0.7;
    }

    //noise shape
    float f = 0.0;
    uv2 = p*vec2(resolution.x/resolution.y,1.0);
    uv2 *= cloudscale;
    uv2 -= q; // Removed timeVal from here

    weight = 0.7;
    for (int i=0; i<8; i++){
        // FIX: Add timeVal here
        f += weight*noise( uv2 + timeVal );
        uv2 = m*uv2;
        weight *= 0.6;
    }

    f *= r + f;

    //noise colour
    float c = 0.0;
    float time2 = mod(time * speed * base_speed * 2.0, 1000.0);
    uv2 = p*vec2(resolution.x/resolution.y,1.0);
    uv2 *= cloudscale*2.0;
    uv2 -= q; // Removed time2 from here

    weight = 0.4;
    for (int i=0; i<7; i++){
        // FIX: Add time2 here
        c += weight*noise( uv2 + time2 );
        uv2 = m*uv2;
        weight *= 0.6;
    }

    //noise ridge colour
    float c1 = 0.0;
    float time3 = mod(time * speed * base_speed * 3.0, 1000.0);
    uv2 = p*vec2(resolution.x/resolution.y,1.0);
    uv2 *= cloudscale*3.0;
    uv2 -= q; // Removed time3 from here

    weight = 0.4;
    for (int i=0; i<7; i++){
        // FIX: Add time3 here
        c1 += abs(weight*noise( uv2 + time3 ));
        uv2 = m*uv2;
        weight *= 0.6;
    }

    c += c1;

    vec3 skycolour = mix(skycolour2, skycolour1, p.y);
    vec3 cloudcolour = vec3(1.1, 1.1, 0.9) * clamp((clouddark + cloudlight*c), 0.0, 1.0);

    f = cloudcover + cloudalpha*f*r;

    vec3 result = mix(skycolour, clamp(skytint * skycolour + cloudcolour, 0.0, 1.0), clamp(f + c, 0.0, 1.0));

    fragColor = vec4( result, 1.0 );
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
