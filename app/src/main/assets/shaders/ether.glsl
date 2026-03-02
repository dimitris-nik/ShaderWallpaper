#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;
uniform float speed;

// Ether by nimitz 2014 (twitter: @stormoid)
// https://www.shadertoy.com/view/MsjSW3
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License

// Define t using uniform time and speed
#define t (time * speed)

mat2 m(float a){float c=cos(a), s=sin(a);return mat2(c,-s,s,c);}

float map(vec3 p){
    p.xz*= m(t*0.4);
    p.xy*= m(t*0.3);
    vec3 q = p*2.+t;
    return length(p+vec3(sin(t*0.7)))*log(length(p)+1.) + sin(q.x+sin(q.z+sin(q.y)))*0.5 - 1.;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ){
    // Original: vec2 p = fragCoord.xy/iResolution.y - vec2(.9,.5);
    // Adjusted for better centering on various aspect ratios (especially portrait)
    vec2 p = (fragCoord.xy - 0.5 * resolution.xy) / resolution.y;

    vec3 cl = vec3(0.);
    float d = 2.5;

    for(int i=0; i<=5; i++)	{
		vec3 p3 = vec3(0,0,5.) + normalize(vec3(p, -1.))*d;
        float rz = map(p3);
		float f =  clamp((rz - map(p3+.1))*0.5, -.1, 1. );
        vec3 l = vec3(0.1,0.3,.4) + vec3(5., 2.5, 3.)*f;
        cl = cl*l + smoothstep(2.5, .0, rz)*.7*l;
		d += min(rz, 1.);
	}
    fragColor = vec4(cl, 1.);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}

