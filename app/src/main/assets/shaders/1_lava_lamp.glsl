#ifdef GL_ES
            precision highp float;
            #endif

            uniform float time;
            uniform vec2 resolution;
            uniform float speed; // User-controlled speed multiplier

            uniform float film_grain; // Matches JSON variable "film_grain"

            // Customizable Colors
            uniform vec3 color1;
            uniform vec3 color2;
            uniform vec3 color3;
            uniform vec3 color4;
            uniform vec3 color5;
            uniform vec3 color6;
            uniform vec3 color7;
            uniform vec3 color8;


            // --- Helper Functions ---

            mat2 Rot(float a) {
                float s = sin(a);
                float c = cos(a);
                return mat2(c, -s, s, c);
            }

            // Mobile-friendly hash function
            vec2 hash(vec2 p) {
                p = vec2(dot(p, vec2(2127.1, 81.17)), dot(p, vec2(1269.5, 283.37)));
                return fract(sin(p) * 43758.5453);
            }

            float noise(in vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);

                vec2 u = f * f * (3.0 - 2.0 * f);

                float n = mix(mix(dot(-1.0 + 2.0 * hash(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0)),
                                  dot(-1.0 + 2.0 * hash(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)), u.x),
                              mix(dot(-1.0 + 2.0 * hash(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)),
                                  dot(-1.0 + 2.0 * hash(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)), u.x), u.y);
                return 0.5 + 0.5 * n;
            }

            float filmGrainNoise(in vec2 co) {
                return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
            }

            // --- Main Function ---

            void main(void) {
                vec2 uv = gl_FragCoord.xy / resolution.xy;
                float aspectRatio = resolution.x / resolution.y;

                // --- TIME & RANDOMNESS LOGIC ---

                // 1. Smooth Animation Time (Fast, creates the waves)
                float smoothTime = time;

                vec2 tuv = uv - 0.5;

                // --- SHAPE GENERATION ---

                // Noise field that warps the space
                float degree = noise(vec2(smoothTime * 0.05 * speed, tuv.x * tuv.y));

                tuv.y *= 1.0 / aspectRatio;
                tuv *= Rot(radians((degree - 0.5) * 720.0 + 180.0));
                tuv.y *= aspectRatio;

                // Layered Wave System
                float frequency = 5.0;
                float amplitude = 30.0;

                // Wave 1 (Main)
                tuv.x += sin(tuv.y * frequency + speed) / amplitude;
                tuv.y += sin(tuv.x * frequency * 1.5 + speed) / (amplitude * 0.5);

                // Wave 2 (Interference - creates complexity)
                tuv.x += sin(tuv.y * 2.1 - speed * 1.5) / (amplitude * 0.8);
                tuv.y += cos(tuv.x * 1.6 + speed * 1.2) / (amplitude * 0.8);

                // --- COLORS ---

                // Colors are now supplied by uniforms color1 through color8.
                // Original logic used 8 colors mixed into 4 transitional colors.
                // We mix them similarly here.

                // Complex Color Mixing
                float slowCycle = sin(smoothTime * 0.1 * speed);
                float fastCycle = sin(smoothTime * 0.3 * speed + 1.5);

                float t1 = (slowCycle * 0.5 + 0.5);
                float t2 = (fastCycle * 0.5 + 0.5);

                // Mix Pair 1: Base Warm (color1) <-> Haze (color5)
                vec3 mixedColor1 = mix(color1, color5, t1);

                // Mix Pair 2: Deep Cool (color2) <-> Dark/Swampy (color6)
                vec3 mixedColor2 = mix(color2, color6, t2);

                // Mix Pair 3: Soft Warm (color3) <-> Accent Warm (color7)
                vec3 mixedColor3 = mix(color3, color7, t1 * t2);

                // Mix Pair 4: Soft Cool (color4) <-> Accent Warm/Dark (color8)
                vec3 mixedColor4 = mix(color4, color8, 1.0 - t1);

                // Blend everything
                vec3 layer1 = mix(mixedColor3, mixedColor2, smoothstep(-0.3, 0.2, (tuv * Rot(radians(-5.0))).x));
                vec3 layer2 = mix(mixedColor4, mixedColor1, smoothstep(-0.3, 0.2, (tuv * Rot(radians(-5.0))).x));

                vec3 color = mix(layer1, layer2, smoothstep(0.5, -0.3, tuv.y));

                // --- FINAL POST PROCESSING ---

                // Static Film Grain (Pixel Locked)
                float noiseVal = filmGrainNoise(gl_FragCoord.xy);
                color = color - noiseVal * film_grain;

                gl_FragColor = vec4(color, 1.0);
            }
