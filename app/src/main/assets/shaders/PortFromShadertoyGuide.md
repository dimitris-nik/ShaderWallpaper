### **Porting Guide: Shadertoy to ShaderWallpaper**

Shadertoy uses WebGL (often equivalent to OpenGL ES 3.0), while this app runs on **OpenGL ES 2.0**. You must adjust GLSL syntax, entry points, and uniform names to match the renderer.

---

## **Quick Start (Minimal Port)**
1. **Add a GLSL file** to `app/src/main/assets/shaders/`.
2. **Create a JSON config** next to it (see schema below).
3. **Replace Shadertoy uniforms** with the app uniforms (`time`, `resolution`, etc.).
4. **Add an ES 2.0 precision header** and a valid `main()`.

**Minimal GLSL template:**
```glsl
#ifdef GL_ES
precision highp float;
#endif

uniform float time;
uniform vec2 resolution;

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    vec3 col = 0.5 + 0.5 * cos(time + vec3(0.0, 2.0, 4.0));
    gl_FragColor = vec4(col, 1.0);
}
```

**Minimal JSON template:**
```json
{
  "name": "Neon Pulse",
  "description": "A vibrating neon effect.",
  "shader_path": "assets/shaders/neon_pulse.glsl",
  "variables": {}
}
```

---

## **1. JSON Configuration (`.json`)**
Every gallery shader needs a JSON file in `app/src/main/assets/shaders/`. This defines the shader name, description, and user-adjustable parameters.

**Required fields**
- `name`: Human-readable shader name.
- `shader_path`: Path to the GLSL file under `assets/`.
- `variables`: A JSON object (empty is valid).

**Optional fields**
- `description`: Short description.
- `tags`: List of tags (e.g., `"resource_intensive"`).
- `credit`: URL or text attribution.

**Variables schema**
```json
"variables": {
"speed": {
"type": "float",
"default": 1.0,
"min": 0.1,
"max": 5.0,
"description": "Animation speed"
},
"glow_color": {
"type": "color",
"default": "#FF00FF"
}
}
```

**How variables map into GLSL**
- `float` becomes `uniform float <name>;`
- `color` becomes `uniform vec3 <name>;` in **0.0 - 1.0** range

---

## **2. GLSL Conversion Rules**
Place your shader in `app/src/main/assets/shaders/`.

### **A. Add Precision Header**
OpenGL ES 2.0 requires explicit precision.
```glsl
#ifdef GL_ES
precision highp float;
#endif
```

### **B. Replace Built-in Uniforms**
`WallpaperRenderer.kt` supplies these built-ins. Use these names in your GLSL:

| Shadertoy | App Uniform | Type | Notes |
| :--- | :--- | :--- | :--- |
| `iTime` / `iGlobalTime` | `time` | `float` | Seconds since start. |
| `iResolution` | `resolution` | `vec2` | Screen size in pixels. |
| `fragCoord` | `gl_FragCoord` | `vec4` | Built-in. Use `.xy`. |
| `iDate` | `date` | `vec4` | Year, Month, Day, Seconds. |
| N/A | `offset` | `vec2` | Parallax from homescreen scroll. |

**Supported time aliases** (already set by the renderer):
- `time`, `u_time`, `iTime`, `uTime`, `iGlobalTime`

Use one of these names in your shader to get time updates.

### **C. Declare Custom Uniforms**
If you define variables in JSON, you must declare them in GLSL:
```glsl
uniform float speed;
uniform vec3 glow_color; // JSON color -> vec3 (0.0 - 1.0)
```

### **D. Entry Point**
Shadertoy uses `mainImage`. You must provide a valid `main()`.

**Shadertoy style:**
```glsl
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    fragColor = vec4(uv, 0.5, 1.0);
}
```

**App style:**
```glsl
void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    vec3 col = vec3(uv, 0.5 + sin(time));
    gl_FragColor = vec4(col, 1.0);
}
```

**Wrapper option (minimal changes):**
```glsl
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Shadertoy code
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
```

---

## **3. OpenGL ES 2.0 Compatibility Notes**
OpenGL ES 2.0 is more limited than Shadertoy. Common fixes:

1. **Texture lookups**
    - ? `texture(CHANNEL, uv)`
    - ? `texture2D(CHANNEL, uv)`
    - Note: This app does **not** provide texture channels.

2. **`in` / `out` qualifiers**
    - ? `in vec2 vUv;`, `out vec4 fragColor;`
    - ? Use `varying` and `gl_FragColor` (ES 2.0 style).

3. **Float literals**
    - ? `float a = 1;`
    - ? `float a = 1.0;`

4. **Vector construction**
    - ? `vec3(0)`
    - ? `vec3(0.0)`

5. **Loops**
    - Some ES 2.0 drivers require **constant loop bounds**.
    - Prefer `for (int i = 0; i < 32; i++)` over uniform-driven bounds.

6. **Derivatives and advanced features**
    - Avoid `dFdx`, `dFdy`, `fwidth`, and advanced extensions unless you add support.

---

## **4. Coordinate Conventions**
- `gl_FragCoord.xy` is in pixels.
- `resolution.xy` is the screen size.
- Common UV pattern:
```glsl
vec2 uv = gl_FragCoord.xy / resolution.xy;
uv = uv * 2.0 - 1.0; // center (-1..1)
uv.x *= resolution.x / resolution.y; // aspect-correct
```

---

## **5. Importing a GLSL File (No JSON)**
If you import a standalone GLSL file in the UI:
- You **do not** get custom parameters (only built-in uniforms).
- Make sure you declare one of the supported time uniforms (`time`, `iTime`, etc.).
- Use `resolution`, `offset`, and `date` if needed.

---

## **6. Troubleshooting**
**Shader is black / not rendering**
- Missing precision header.
- `main()` missing or no `gl_FragColor` written.
- Compilation error (reduce code, then add back gradually).

**`time` does not animate**
- Ensure you declared `uniform float time;` (or an alias) and use it.
- If you use `iTime`, confirm the uniform is named exactly `iTime`.
- If the time value is unused, some drivers optimize it away.

**Colors look wrong**
- Colors from JSON are **vec3** in 0.0 - 1.0.
- If you use `vec4`, set alpha explicitly.

---

## **7. Performance Tips**
- Reduce raymarching steps and nested loops.
- Avoid heavy trig in large loops.
- Prefer cheaper noise approximations.
- Keep per-fragment work low for battery and smooth scrolling.

---

## **8. Testing Checklist**
1. Defaults look good with no parameter changes.
2. Animation works (time updates visibly).
3. Aspect ratio is correct on tall screens.
4. Performance is acceptable on a mid-range device.
