#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;

// User configurable uniforms
uniform float speed_val;       // Maps to SPIN_SPEED
uniform float rotation_speed;  // Maps to SPIN_ROTATION
uniform float pixel_filter;    // Maps to PIXEL_FILTER
uniform float spin_amount;     // Maps to SPIN_AMOUNT
uniform float contrast;        // Maps to CONTRAST
uniform vec3 primary_color;    // Maps to COLOUR_1
uniform vec3 secondary_color;  // Maps to COLOUR_2

// Constants
#define OFFSET vec2(0.0)
#define COLOUR_3 vec4(0.086, 0.137, 0.145, 1.0) // Keeping dark background constant for now, or could be exposed
#define LIGTHING 0.4
#define SPIN_EASE 1.0
#define PI 3.14159265359
// We will enable rotation by default in logic if rotation_speed is non-zero, or just follow the math

vec4 effect(vec2 screenSize, vec2 screen_coords) {
    float pixel_size = length(screenSize.xy) / pixel_filter;
    vec2 uv = (floor(screen_coords.xy*(1.0/pixel_size))*pixel_size - 0.5*screenSize.xy)/length(screenSize.xy) - OFFSET;
    float uv_len = length(uv);

    // Rotation logic
    // variable 'speed' here determines the initial angle offset
    // The original code had IS_ROTATE check. We can just use time * rotation_speed
    float speed = (rotation_speed * SPIN_EASE * 0.2);
    // If we want it to rotate over time:
    speed = time * speed;

    speed += 302.2;
    float new_pixel_angle = atan(uv.y, uv.x) + speed - SPIN_EASE*20.0*(1.0*spin_amount*uv_len + (1.0 - 1.0*spin_amount));
    vec2 mid = (screenSize.xy/length(screenSize.xy))/2.0;
    uv = (vec2((uv_len * cos(new_pixel_angle) + mid.x), (uv_len * sin(new_pixel_angle) + mid.y)) - mid);

    uv *= 30.0;

    // Animation speed
    speed = time * speed_val;
    vec2 uv2 = vec2(uv.x+uv.y);

    for(int i=0; i < 5; i++) {
        uv2 += sin(max(uv.x, uv.y)) + uv;
        uv  += 0.5*vec2(cos(5.1123314 + 0.353*uv2.y + speed*0.131121),sin(uv2.x - 0.113*speed));
        uv  -= 1.0*cos(uv.x + uv.y) - 1.0*sin(uv.x*0.711 - uv.y);
    }

    float contrast_mod = (0.25*contrast + 0.5*spin_amount + 1.2);
    float paint_res = min(2.0, max(0.0,length(uv)*(0.035)*contrast_mod));
    float c1p = max(0.0,1.0 - contrast_mod*abs(1.0-paint_res));
    float c2p = max(0.0,1.0 - contrast_mod*abs(paint_res));
    float c3p = 1.0 - min(1.0, c1p + c2p);
    float light = (LIGTHING - 0.2)*max(c1p*5.0 - 4.0, 0.0) + LIGTHING*max(c2p*5.0 - 4.0, 0.0);

    vec4 col1 = vec4(primary_color, 1.0);
    vec4 col2 = vec4(secondary_color, 1.0);

    return (0.3/contrast)*col1 + (1.0 - 0.3/contrast)*(col1*c1p + col2*c2p + vec4(c3p*COLOUR_3.rgb, c3p*col1.a)) + light;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    fragColor = effect(resolution.xy, fragCoord.xy);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}


