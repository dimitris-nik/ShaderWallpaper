#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;
uniform float speed;
uniform vec3 color1;
uniform vec3 color2;
uniform vec3 color3; // New 3rd color
uniform float count; // Used to control loop max

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // Fix aspect ratio
	vec2 uv = (2.0*fragCoord-resolution.xy) / resolution.y;

    // Background (using white/grey gradient)
	vec3 color = vec3(0.8 + 0.2*uv.y);

    // Bubbles
	for( int i=0; i<40; i++ )
	{
        // break if we exceed user count to optimize perf (if valid in driver)
        if (float(i) > count) break;

        // bubble seeds
		float pha =      sin(float(i)*546.13+1.0)*0.5 + 0.5;
		float siz = pow( sin(float(i)*651.74+5.0)*0.5 + 0.5, 4.0 );
		float pox =      sin(float(i)*321.55+4.1) * resolution.x / resolution.y;

        // bubble size, position and color
		float rad = 0.1 + 0.5*siz;
		vec2  pos = vec2( pox, -1.0-rad + (2.0+2.0*rad)*mod(pha+0.1*(time*speed)*(0.2+0.8*siz),1.0));
		float dis = length( uv - pos );

        // User customized colors with 3-way mix
        float mixVal = 0.5+0.5*sin(float(i)*1.2+1.9);
        vec3 col;
        if (mixVal < 0.5) {
            col = mix(color1, color2, mixVal * 2.0);
        } else {
            col = mix(color2, color3, (mixVal - 0.5) * 2.0);
        }

        // render
		float f = length(uv-pos)/rad;
		f = sqrt(clamp(1.0-f*f,0.0,1.0));

        // Soft additive blending instead of subtractive
        // Calculate bubble opacity/shape
        float shape = (1.0-smoothstep( rad*0.95, rad, dis )) * f;

        // Mix the bubble color on top of existing color
        // Using mix prevents it from getting too dark (subtractive) or blown out (pure additive)
        color = mix(color, col, shape * 0.8); // 0.8 transparency
	}

    // vigneting
	color *= sqrt(1.5-0.5*length(uv));

	fragColor = vec4(color,1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
