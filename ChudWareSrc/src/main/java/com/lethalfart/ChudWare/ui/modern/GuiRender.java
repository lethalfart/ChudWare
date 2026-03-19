package com.lethalfart.ChudWare.ui.modern;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import com.lethalfart.ChudWare.ui.modern.shader.RoundedRectShader;
import org.lwjgl.opengl.GL11;

public final class GuiRender
{
    private GuiRender() {}
    private static int screenHeight = 0;
    private static int scaleFactor = 1;

    public static void drawRect(int x1, int y1, int x2, int y2, int color)
    {
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    public static void drawBorder(int x, int y, int w, int h, int color)
    {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }

    public static void drawSoftShadow(int x, int y, int w, int h, int color)
    {
        drawSoftShadow(x, y, w, h, 0, color);
    }

    public static void drawSoftShadow(int x, int y, int w, int h, int radius, int color)
    {
        for (int i = 1; i <= 6; i++)
        {
            int c = withAlpha(color, 0.08f * (1f - (i - 1) / 6f));
            drawRoundedRect(x - i, y - i, w + (i * 2), h + (i * 2), radius + i, c);
        }
    }

    public static void drawRoundedRect(int x, int y, int w, int h, int radius, int color)
    {
        if (w <= 0 || h <= 0) return;
        int r = Math.min(Math.max(0, radius), Math.min(w, h) / 2);
        boolean wasTex = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasAlpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean wasDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        if (RoundedRectShader.isAvailable())
        {
            RoundedRectShader.draw(x, y, w, h, r, color);
            restoreState(wasTex, wasBlend, wasAlpha, wasDepth, wasCull, wasDepthMask);
            return;
        }
        if (r <= 0)
        {
            drawRect(x, y, x + w, y + h, color);
            restoreState(wasTex, wasBlend, wasAlpha, wasDepth, wasCull, wasDepthMask);
            return;
        }

        drawRect(x + r, y, x + w - r, y + h, color);
        drawRect(x, y + r, x + w, y + h - r, color);

        float a = ((color >> 24) & 0xFF) / 255f;
        float cr = ((color >> 16) & 0xFF) / 255f;
        float cg = ((color >> 8) & 0xFF) / 255f;
        float cb = (color & 0xFF) / 255f;

        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GlStateManager.color(cr, cg, cb, a);

        drawCorner(x + r, y + r, r, 180, 270);
        drawCorner(x + w - r, y + r, r, 270, 360);
        drawCorner(x + w - r, y + h - r, r, 0, 90);
        drawCorner(x + r, y + h - r, r, 90, 180);

        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        restoreState(wasTex, wasBlend, wasAlpha, wasDepth, wasCull, wasDepthMask);
    }

    private static void restoreState(boolean tex, boolean blend, boolean alpha, boolean depth, boolean cull, boolean depthMask)
    {
        if (tex) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        GlStateManager.depthMask(depthMask);
    }

    private static void drawCorner(int cx, int cy, int r, int startDeg, int endDeg)
    {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        int step = 4;
        for (int a = startDeg; a <= endDeg; a += step)
        {
            double rad = Math.toRadians(a);
            GL11.glVertex2f((float)(cx + Math.cos(rad) * r), (float)(cy + Math.sin(rad) * r));
        }
        double rad = Math.toRadians(endDeg);
        GL11.glVertex2f((float)(cx + Math.cos(rad) * r), (float)(cy + Math.sin(rad) * r));
        GL11.glEnd();
    }

    public static void drawRoundedBorder(int x, int y, int w, int h, int radius, int borderColor, int fillColor)
    {
        if (w <= 0 || h <= 0) return;
        if (w <= 2 || h <= 2)
        {
            drawRoundedRect(x, y, w, h, radius, borderColor);
            return;
        }
        drawRoundedRect(x, y, w, h, radius, borderColor);
        drawRoundedRect(x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fillColor);
    }

    public static int withAlpha(int color, float alpha)
    {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int na = Math.min(255, Math.max(0, Math.round(a * alpha)));
        return (na << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lerpColor(int a, int b, float t)
    {
        t = clamp(t, 0f, 1f);
        int aA = (a >> 24) & 0xFF;
        int aR = (a >> 16) & 0xFF;
        int aG = (a >> 8) & 0xFF;
        int aB = a & 0xFF;

        int bA = (b >> 24) & 0xFF;
        int bR = (b >> 16) & 0xFF;
        int bG = (b >> 8) & 0xFF;
        int bB = b & 0xFF;

        int oA = (int)(aA + (bA - aA) * t);
        int oR = (int)(aR + (bR - aR) * t);
        int oG = (int)(aG + (bG - aG) * t);
        int oB = (int)(aB + (bB - aB) * t);

        return (oA << 24) | (oR << 16) | (oG << 8) | oB;
    }

    public static float clamp(float v, float min, float max)
    {
        return v < min ? min : (v > max ? max : v);
    }

    public static void beginScissor(ScaledResolution sr, int x, int y, int w, int h)
    {
        int scale = sr.getScaleFactor();
        int sx = x * scale;
        int sy = (sr.getScaledHeight() - (y + h)) * scale;
        int sw = w * scale;
        int sh = h * scale;
        if (sw < 0) sw = 0;
        if (sh < 0) sh = 0;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }

    public static void endScissor()
    {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void enableBlend()
    {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void setScreenHeight(int height)
    {
        screenHeight = height;
    }

    public static void setScreenMetrics(int scaledHeight, int scale)
    {
        scaleFactor = Math.max(1, scale);
        screenHeight = scaledHeight * scaleFactor;
    }
}
