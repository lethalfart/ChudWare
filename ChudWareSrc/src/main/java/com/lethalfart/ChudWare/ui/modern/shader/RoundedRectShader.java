package com.lethalfart.ChudWare.ui.modern.shader;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class RoundedRectShader
{
    private static int program = 0;
    private static int uPos;
    private static int uSize;
    private static int uRadius;
    private static int uColor;
    private static boolean failed = false;

    private RoundedRectShader() {}

    public static boolean isAvailable()
    {
        ensureInit();
        return program != 0 && !failed;
    }

    public static void draw(int x, int y, int w, int h, int radius, int color)
    {
        if (!isAvailable()) return;
        if (w <= 0 || h <= 0) return;

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GL20.glUseProgram(program);
        GL20.glUniform2f(uPos, x, y);
        GL20.glUniform2f(uSize, w, h);
        GL20.glUniform1f(uRadius, radius);
        GL20.glUniform4f(uColor, r, g, b, a);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x, y, 0).endVertex();
        wr.pos(x + w, y, 0).endVertex();
        wr.pos(x + w, y + h, 0).endVertex();
        wr.pos(x, y + h, 0).endVertex();
        tess.draw();

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
                System.out.println("[ChudWare] RoundedRectShader compile failed.");
                return;
            }
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vs);
            GL20.glAttachShader(program, fs);
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            {
                String info = GL20.glGetProgramInfoLog(program, 1024);
                System.out.println("[ChudWare] RoundedRectShader link failed: " + info);
                failed = true;
                program = 0;
                return;
            }

            uPos = GL20.glGetUniformLocation(program, "u_pos");
            uSize = GL20.glGetUniformLocation(program, "u_size");
            uRadius = GL20.glGetUniformLocation(program, "u_radius");
            uColor = GL20.glGetUniformLocation(program, "u_color");
            if (uPos < 0 || uSize < 0 || uRadius < 0 || uColor < 0)
            {
                System.out.println("[ChudWare] RoundedRectShader uniforms missing (pos=" + uPos +
                        ", size=" + uSize + ", radius=" + uRadius + ", color=" + uColor + ").");
                failed = true;
                program = 0;
            }
        }
        catch (Throwable t)
        {
            System.out.println("[ChudWare] RoundedRectShader init failed: " + t.getMessage());
            failed = true;
            program = 0;
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
            System.out.println("[ChudWare] RoundedRectShader compile error: " + info);
            return 0;
        }
        return shader;
    }

    private static final String VERTEX_SRC =
            "#version 120\n" +
            "varying vec2 v_pos;\n" +
            "void main() {\n" +
            "  gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
            "  v_pos = gl_Vertex.xy;\n" +
            "}\n";

    private static final String FRAG_SRC =
            "#version 120\n" +
            "uniform vec2 u_pos;\n" +
            "uniform vec2 u_size;\n" +
            "uniform float u_radius;\n" +
            "uniform vec4 u_color;\n" +
            "varying vec2 v_pos;\n" +
            "\n" +
            "float roundedBoxSDF(vec2 p, vec2 b, float r) {\n" +
            "  vec2 q = abs(p) - b + vec2(r);\n" +
            "  return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "  vec2 frag = v_pos - u_pos;\n" +
            "  vec2 halfSize = u_size * 0.5;\n" +
            "  vec2 p = frag - halfSize;\n" +
            "  float r = min(u_radius, min(u_size.x, u_size.y) * 0.5);\n" +
            "  float d = roundedBoxSDF(p, halfSize, r);\n" +
            "  float aa = 1.5;\n" +
            "  float alpha = 1.0 - smoothstep(0.0, aa, d);\n" +
            "  gl_FragColor = vec4(u_color.rgb, u_color.a * alpha);\n" +
            "}\n";
}
