package com.lethalfart.ChudWare.module.impl.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class ChamsShader
{
    private static int program = 0;
    private static int uColor;
    private static int uGlow;
    private static int uGlowStrength;
    private static int uRimPower;
    private static boolean failed = false;

    private ChamsShader() {}

    public static boolean isAvailable()
    {
        ensureInit();
        return program != 0 && !failed;
    }

    public static void use(int color, int glow, float glowStrength, float rimPower)
    {
        if (!isAvailable()) return;

        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        float ga = ((glow >>> 24) & 0xFF) / 255f;
        float gr = ((glow >>> 16) & 0xFF) / 255f;
        float gg = ((glow >>> 8) & 0xFF) / 255f;
        float gb = (glow & 0xFF) / 255f;

        GL20.glUseProgram(program);
        GL20.glUniform4f(uColor, r, g, b, a);
        GL20.glUniform4f(uGlow, gr, gg, gb, ga);
        GL20.glUniform1f(uGlowStrength, glowStrength);
        GL20.glUniform1f(uRimPower, rimPower);
    }

    public static void stop()
    {
        GL20.glUseProgram(0);
    }

    private static void ensureInit()
    {
        if (program != 0 || failed) return;
        try
        {
            int vs = compile(GL20.GL_VERTEX_SHADER, VERTEX_SRC);
            int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAG_SRC);
            if (vs == 0 || fs == 0)
            {
                failed = true;
                System.out.println("[ChudWare] ChamsShader compile failed.");
                return;
            }
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vs);
            GL20.glAttachShader(program, fs);
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            {
                String info = GL20.glGetProgramInfoLog(program, 1024);
                System.out.println("[ChudWare] ChamsShader link failed: " + info);
                failed = true;
                program = 0;
                return;
            }

            uColor = GL20.glGetUniformLocation(program, "u_color");
            uGlow = GL20.glGetUniformLocation(program, "u_glow");
            uGlowStrength = GL20.glGetUniformLocation(program, "u_glowStrength");
            uRimPower = GL20.glGetUniformLocation(program, "u_rimPower");
        }
        catch (Throwable t)
        {
            failed = true;
            program = 0;
            System.out.println("[ChudWare] ChamsShader init failed: " + t.getMessage());
        }
    }

    private static int compile(int type, String src)
    {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
        {
            String info = GL20.glGetShaderInfoLog(shader, 1024);
            System.out.println("[ChudWare] ChamsShader compile error: " + info);
            return 0;
        }
        return shader;
    }

    private static final String VERTEX_SRC =
            "#version 120\n" +
            "varying float v_ndotv;\n" +
            "void main() {\n" +
            "  vec3 n = normalize(gl_NormalMatrix * gl_Normal);\n" +
            "  vec3 v = normalize(-(gl_ModelViewMatrix * gl_Vertex).xyz);\n" +
            "  v_ndotv = clamp(dot(n, v), 0.0, 1.0);\n" +
            "  gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
            "}\n";

    private static final String FRAG_SRC =
            "#version 120\n" +
            "uniform vec4 u_color;\n" +
            "uniform vec4 u_glow;\n" +
            "uniform float u_glowStrength;\n" +
            "uniform float u_rimPower;\n" +
            "varying float v_ndotv;\n" +
            "void main() {\n" +
            "  float rim = pow(1.0 - v_ndotv, u_rimPower) * u_glowStrength;\n" +
            "  vec3 rgb = u_color.rgb + (u_glow.rgb * rim);\n" +
            "  float a = clamp(u_color.a + (u_glow.a * rim), 0.0, 1.0);\n" +
            "  gl_FragColor = vec4(rgb, a);\n" +
            "}\n";
}
