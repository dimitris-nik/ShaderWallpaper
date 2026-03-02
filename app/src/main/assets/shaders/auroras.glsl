#ifdef GL_ES
precision highp float;
#endif

// --- Standard Uniforms ---
uniform float time;
uniform vec2 resolution;
uniform float speed; // User controlled speed (default 1.0)

// --- Defines & Helper Functions ---
// Convert iTime to 'time' uniformly
#define iTime (time * speed)
#define iResolution resolution

// Matrix helpers
mat2 mm2(in float a){float c = cos(a), s = sin(a);return mat2(c,s,-s,c);}
mat2 m2 = mat2(0.95534, 0.29552, -0.29552, 0.95534);
float tri(in float x){return clamp(abs(fract(x)-.5),0.01,0.49);}
vec2 tri2(in vec2 p){return vec2(tri(p.x)+tri(p.y),tri(p.y+tri(p.x)));}

// --- Noise Functions ---
float triNoise2d(in vec2 p, float spd) {
    float z=1.8;
    float z2=2.5;
    float rz = 0.;
    p *= mm2(p.x*0.06);
    vec2 bp = p;
    for (float i=0.; i<5.; i++ )
    {
        vec2 dg = tri2(bp*1.85)*.75;
        dg *= mm2(iTime*spd);
        p -= dg/z2;

        bp *= 1.3;
        z2 *= .45;
        z *= .42;
        p *= 1.21 + (rz-1.0)*.02;

        rz += tri(p.x+tri(p.y))*z;
        p*= -m2;
    }
    return clamp(1./pow(rz*29., 1.3),0.,.55);
}

// 2D Hash
float hash21(in vec2 n){ return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453); }

// 3D Hash (unsigned integer based hash polyfill for GLES 2.0)
// Original used uvec3/ivec3 which aren't always available in GLES 2.0 unless extensions are present.
// We will replace nmzHash33 with a float-based hash for compatibility if needed.
// However, many modern GLES 2.0 implementations support bitwise ops or we can use a float replacement.
// Let's try to stick to a float-based hash to be safe for all older devices.
// Replacing 'nmzHash33' with a standard float hash function.

vec3 hash33(vec3 p3) {
	p3 = fract(p3 * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yxz+33.33);
    return fract((p3.xxy + p3.yxx)*p3.zyx);
}

// --- Effects ---

vec4 aurora(vec3 ro, vec3 rd) {
    vec4 col = vec4(0);
    vec4 avgCol = vec4(0);

    for(float i=0.;i<50.;i++)
    {
        float of = 0.006*hash21(gl_FragCoord.xy)*smoothstep(0.,15., i);
        float pt = ((.8+pow(i,1.4)*.002)-ro.y)/(rd.y*2.+0.4);
        pt -= of;
        vec3 bpos = ro + pt*rd;
        vec2 p = bpos.zx;
        float rzt = triNoise2d(p, 0.06);
        vec4 col2 = vec4(0,0,0, rzt);
        col2.rgb = (sin(1.-vec3(2.15,-.5, 1.2)+i*0.043)*0.5+0.5)*rzt;
        avgCol =  mix(avgCol, col2, .5);
        col += avgCol*exp2(-i*0.065 - 2.5)*smoothstep(0.,5., i);

    }

    col *= (clamp(rd.y*15.+.4,0.,1.));
    return col*1.8;
}

vec3 stars(in vec3 p) {
    vec3 c = vec3(0.);
    float res = iResolution.x*1.;

    for (float i=0.;i<4.;i++)
    {
        vec3 q = fract(p*(.15*res))-0.5;
        vec3 id = floor(p*(.15*res));
        // vec2 rn = nmzHash33(id).xy; // Original
        vec2 rn = hash33(id).xy; // Replacement
        float c2 = 1.-smoothstep(0.,.6,length(q));
        c2 *= step(rn.x,.0005+i*i*0.001);
        c += c2*(mix(vec3(1.0,0.49,0.1),vec3(0.75,0.9,1.),rn.y)*0.1+0.9);
        p *= 1.3;
    }
    return c*c*.8;
}

vec3 bg(in vec3 rd) {
    float sd = dot(normalize(vec3(-0.5, -0.6, 0.9)), rd)*0.5+0.5;
    sd = pow(sd, 5.);
    vec3 col = mix(vec3(0.05,0.1,0.2), vec3(0.1,0.05,0.2), sd);
    return col*.63;
}

// --- Main ---

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 q = fragCoord.xy / iResolution.xy;
    vec2 p = q - 0.5;

    // PORTRAIT FIX:
    // Original: p.x*=iResolution.x/iResolution.y; (Aspect Correct Width)
    // For portrait phones, aspect ratio (w/h) is e.g. 0.5.
    // This squishes the X axis. For portrait, we might prefer normalizing Y instead to keep the horizon low.
    // Or we keep the aspect ratio logic but rotate the camera slightly.

    // Let's stick to standard aspect correction first.
    p.x *= iResolution.x/iResolution.y;

    vec3 ro = vec3(0,0,-6.7);
    vec3 rd = normalize(vec3(p,1.3));

    // MOUSE REPLACEMENT:
    // Original used active mouse to rotate view.
    // We replace 'mo' with a slow automatic pan or fixed view suitable for portrait.
    // vec2 mo = iMouse.xy / iResolution.xy-.5;
    // Automatic gentle sway
    vec2 mo = vec2(-0.1, 0.1);
    mo.x += sin(iTime*0.1)*0.05; // Gentle horizontal sway

    // Correct mouse aspect if needed, though for auto-pan it matters less
    mo.x *= iResolution.x/iResolution.y;

    rd.yz *= mm2(mo.y);
    rd.xz *= mm2(mo.x + sin(iTime*0.05)*0.2);

    vec3 col = vec3(0.);
    vec3 brd = rd;
    float fade = smoothstep(0.,0.01,abs(brd.y))*0.1+0.9;

    col = bg(rd)*fade;

    if (rd.y > 0.){
        vec4 aur = smoothstep(0.,1.5,aurora(ro,rd))*fade;
        col += stars(rd);
        col = col*(1.-aur.a) + aur.rgb;
    }
    else //Reflections
    {
        rd.y = abs(rd.y);
        col = bg(rd)*fade*0.6;
        vec4 aur = smoothstep(0.0,2.5,aurora(ro,rd));
        col += stars(rd)*0.1;
        col = col*(1.-aur.a) + aur.rgb;
        vec3 pos = ro + ((0.5-ro.y)/rd.y)*rd;
        float nz2 = triNoise2d(pos.xz*vec2(.5,.7), 0.);
        col += mix(vec3(0.2,0.25,0.5)*0.08,vec3(0.3,0.3,0.5)*0.7, nz2*0.4);
    }

    fragColor = vec4(col, 1.);
}

// --- Entry Point ---
void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}

