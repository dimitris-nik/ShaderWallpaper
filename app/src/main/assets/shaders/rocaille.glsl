#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;
uniform float speed;
uniform float scale;

// Tanh approximation for GLES 2.0
// tanh(x) = (exp(2x) - 1) / (exp(2x) + 1)
vec4 tanh_approx(vec4 x) {
    vec4 ex = exp(2.0 * x);
    return (ex - 1.0) / (ex + 1.0);
}

void mainImage(out vec4 O, vec2 I) {
    //Vector for scaling and turbulence
    vec2 v = resolution.xy;

    //Centered and scaled coordinates
    // Original: p = (I+I-v)/v.y/.3;
    // We use 'scale' uniform. Default should be 0.3 to match original.
    vec2 p = (I+I-v)/v.y / scale;

    //Iterators for layers and turbulence frequency
    float i = 0.0;
    float f = 0.0;

    O = vec4(0.0);

    // Original loop: for(O*=i;i++<9.; ... )
    // i starts at 0. Check (0<9) -> True, i becomes 1. Body runs with i=1.
    // ...
    // Check (8<9) -> True, i becomes 9. Body runs with i=9.
    // So loop runs with i = 1.0, 2.0, ... 9.0

    for(i=1.0; i<=9.0; i++) {
        //Turbulence loop
        v = p;
        // Inner loop: for(v=p,f=0.;f++<9.;v+=sin(v.yx*f+i+iTime)/f);
        // f starts 0. Check (0<9) -> True, f becomes 1. Body runs with f=1.
        for(f=1.0; f<=9.0; f++) {
            v += sin(v.yx * f + i + time * speed) / f;
        }

        //Add coloring, attenuating with turbulent coordinates
        O += (cos(i + vec4(0,1,2,3)) + 1.0) / 6.0 / length(v);
    }

    //Tanh tonemapping
    O = tanh_approx(O*O);
    O.a = 1.0;
}

void main() {
    vec4 color = vec4(0.0);
    mainImage(color, gl_FragCoord.xy);
    gl_FragColor = color;
}

