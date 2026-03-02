#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;
uniform float speed;      // User controlled speed
uniform float scale;      // User controlled scale

// Math constants
#define PI 3.14159265359
#define MAX 10000.0

// Ray intersects sphere
// e = -b +/- sqrt( b^2 - c )
vec2 ray_vs_sphere( vec3 p, vec3 dir, float r ) {
	float b = dot( p, dir );
	float c = dot( p, p ) - r * r;

	float d = b * b - c;
	if ( d < 0.0 ) {
		return vec2( MAX, -MAX );
	}
	d = sqrt( d );

	return vec2( -b - d, -b + d );
}

// Mie phase function
float phase_mie( float g, float c, float cc ) {
	float gg = g * g;

	float a = ( 1.0 - gg ) * ( 1.0 + cc );

	float b = 1.0 + gg - 2.0 * g * c;
	b *= sqrt( b );
	b *= 2.0 + gg;

	return ( 3.0 / 8.0 / PI ) * a / b;
}

// Rayleigh phase function
float phase_ray( float cc ) {
	return ( 3.0 / 16.0 / PI ) * ( 1.0 + cc );
}

// Scatter constants
// Standardizing constants to floats for ES 2.0 safety
#define R_INNER 1.0
#define R 1.5
#define NUM_OUT_SCATTER 8
#define NUM_IN_SCATTER 40
// Reduced IN_SCATTER from 80 to 40 for mobile performance.
// 80 iterations per pixel is heavy for a background wallpaper.

float density( vec3 p, float ph ) {
	return exp( -max( length( p ) - R_INNER, 0.0 ) / ph );
}

float optic( vec3 p, vec3 q, float ph ) {
	vec3 s = ( q - p ) / float( NUM_OUT_SCATTER );
	vec3 v = p + s * 0.5;

	float sum = 0.0;
	for ( int i = 0; i < NUM_OUT_SCATTER; i++ ) {
		sum += density( v, ph );
		v += s;
	}
	sum *= length( s );

	return sum;
}

vec3 in_scatter( vec3 o, vec3 dir, vec2 e, vec3 l ) {
	const float ph_ray = 0.05;
    const float ph_mie = 0.02;

    // vec3 constants must be constructed
    const vec3 k_ray = vec3( 3.8, 13.5, 33.1 );
    const vec3 k_mie = vec3( 21.0, 21.0, 21.0 );
    const float k_mie_ex = 1.1;

	vec3 sum_ray = vec3( 0.0 );
    vec3 sum_mie = vec3( 0.0 );

    float n_ray0 = 0.0;
    float n_mie0 = 0.0;

	float len = ( e.y - e.x ) / float( NUM_IN_SCATTER );
    vec3 s = dir * len;
	vec3 v = o + dir * ( e.x + len * 0.5 );

    for ( int i = 0; i < NUM_IN_SCATTER; i++ ) {
   		float d_ray = density( v, ph_ray ) * len;
        float d_mie = density( v, ph_mie ) * len;

        n_ray0 += d_ray;
        n_mie0 += d_mie;

        // #if 0 block removed

        vec2 f = ray_vs_sphere( v, l, R );
		vec3 u = v + l * f.y;

        float n_ray1 = optic( v, u, ph_ray );
        float n_mie1 = optic( v, u, ph_mie );

        vec3 att = exp( - ( n_ray0 + n_ray1 ) * k_ray - ( n_mie0 + n_mie1 ) * k_mie * k_mie_ex );

		sum_ray += d_ray * att;
        sum_mie += d_mie * att;
        v += s; // Moved increment here for ES 2.0 loop compatibility in some compilers
	}

	float c  = dot( dir, -l );
	float cc = c * c;
    vec3 scatter =
        sum_ray * k_ray * phase_ray( cc ) +
     	sum_mie * k_mie * phase_mie( -0.78, c, cc );

	return 10.0 * scatter;
}

// angle : pitch, yaw
mat3 rot3xy( vec2 angle ) {
	vec2 c = cos( angle );
	vec2 s = sin( angle );

	return mat3(
		c.y      ,  0.0, -s.y,
		s.y * s.x,  c.x,  c.y * s.x,
		s.y * c.x, -s.x,  c.y * c.x
	);
}

// ray direction
vec3 solve_ray_dir( float fov, vec2 size, vec2 pos ) {
	vec2 xy = pos - size * 0.5;
	float cot_half_fov = tan( radians( 90.0 - fov * 0.5 ) );
	float z = size.y * 0.5 * cot_half_fov;

	return normalize( vec3( xy, -z ) );
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
	// default ray dir
	vec3 dir = solve_ray_dir( 45.0, resolution.xy, fragCoord.xy );

	// default ray origin
    float s = max(scale, 0.1);
	vec3 eye = vec3( 0.0, 0.0, 5.0 / s );

	// rotate camera
    // Using user defined speed
	mat3 rot = rot3xy( vec2( 0.0, time * speed * 0.5 ) );
	dir = rot * dir;
	eye = rot * eye;

	// sun light dir
	vec3 l = vec3( 0.0, 0.0, 1.0 );

	vec2 e = ray_vs_sphere( eye, dir, R );
	if ( e.x > e.y ) {
		fragColor = vec4( 0.0, 0.0, 0.0, 1.0 );
        return;
	}

	vec2 f = ray_vs_sphere( eye, dir, R_INNER );
	e.y = min( e.y, f.x );

	vec3 I = in_scatter( eye, dir, e, l );

	fragColor = vec4( pow( I, vec3( 1.0 / 2.2 ) ), 1.0 );
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
