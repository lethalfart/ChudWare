package com.lethalfart.ChudWare.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class ModernFontRenderer
{
    public static final int SIZE_TINY   = 11;
    public static final int SIZE_SMALL  = 13;
    public static final int SIZE_NORMAL = 16;
    public static final int SIZE_MEDIUM = 18;
    public static final int SIZE_LARGE  = 22;
    public static final int SIZE_HEADER = 26;
    private static final float ATLAS_SCALE = 2.0f;

    private static final String CHARS =
            " !\"#$%&'()*+,-./0123456789:;<=>?" +
                    "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`" +
                    "abcdefghijklmnopqrstuvwxyz{|}~";

    private final Map<Integer, GlyphAtlas> atlases = new HashMap<>();
    private Font baseFont = null;
    private boolean failed = false;

    public ModernFontRenderer(Minecraft mc)
    {
        try
        {
            InputStream is = getClass().getResourceAsStream("/assets/chudware/fonts/Inter.ttf");
            if (is == null)
                is = getClass().getResourceAsStream("/assets/chudware/fonts/inter.ttf");

            if (is != null)
            {
                baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
            }
            else
            {
                baseFont = new Font("Arial", Font.PLAIN, 16);
                System.out.println("[ChudWare] Inter.ttf not found, falling back to Arial");
            }
        }
        catch (Throwable e)
        {
            failed = true;
            System.out.println("[ChudWare] Font load failed: " + e.getMessage());
        }
    }

    public void drawString(String text, float x, float y, int color, int size)
    {
        if (text == null || text.isEmpty()) return;
        GlyphAtlas atlas = getAtlas(size);
        if (atlas == null) { fallback(text, x, y, color); return; }
        renderText(text, x, y, color, atlas);
    }

    public void drawStringWithShadow(String text, float x, float y, int color, int size)
    {
        drawString(text, x + 1, y + 1, 0x55000000, size);
        drawString(text, x, y, color, size);
    }

    public void drawCenteredString(String text, float cx, float y, int color, int size)
    {
        drawString(text, cx - getStringWidth(text, size) / 2f, y, color, size);
    }

    public void drawCenteredStringWithShadow(String text, float cx, float y, int color, int size)
    {
        drawStringWithShadow(text, cx - getStringWidth(text, size) / 2f, y, color, size);
    }

    public int getStringWidth(String text, int size)
    {
        if (text == null || text.isEmpty()) return 0;
        GlyphAtlas atlas = getAtlas(size);
        if (atlas == null)
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        int w = 0;
        for (int i = 0; i < text.length(); i++)
        {
            GlyphInfo g = atlas.glyphs.get(text.charAt(i));
            w += (g != null) ? g.advance : atlas.spaceAdvance;
        }
        return w;
    }

    public int getFontHeight(int size)
    {
        GlyphAtlas atlas = getAtlas(size);
        if (atlas == null)
            return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        return atlas.lineHeight;
    }

    public boolean isReady() { return !failed && baseFont != null; }

    private void renderText(String text, float x, float y, int color, GlyphAtlas atlas)
    {
        float rx = (float) Math.round(x);
        float ry = (float) Math.round(y);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0f) a = 1f;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01f);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.color(r, g, b, a);
        GL11.glColor4f(r, g, b, a);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas.textureId);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        float cx = rx;
        for (int i = 0; i < text.length(); i++)
        {
            char ch = text.charAt(i);
            GlyphInfo gi = atlas.glyphs.get(ch);
            if (gi == null) { cx += atlas.spaceAdvance; continue; }

            float x0 = cx;
            float y0 = ry;
            float x1 = cx + gi.w;
            float y1 = ry + gi.h;

            wr.pos(x0, y1, 0).tex(gi.u0, gi.v1).endVertex();
            wr.pos(x1, y1, 0).tex(gi.u1, gi.v1).endVertex();
            wr.pos(x1, y0, 0).tex(gi.u1, gi.v0).endVertex();
            wr.pos(x0, y0, 0).tex(gi.u0, gi.v0).endVertex();

            cx += gi.advance;
        }

        tess.draw();

        GlStateManager.popMatrix();
        GL11.glPopAttrib();
    }

    private GlyphAtlas getAtlas(int size)
    {
        if (failed || baseFont == null) return null;
        GlyphAtlas cached = atlases.get(size);
        if (cached != null) return cached;
        try
        {
            GlyphAtlas atlas = buildAtlas(size);
            atlases.put(size, atlas);
            return atlas;
        }
        catch (Throwable e)
        {
            System.out.println("[ChudWare] Atlas build failed size=" + size + ": " + e.getMessage());
            return null;
        }
    }

    private GlyphAtlas buildAtlas(int size) throws Exception
    {
        float scaledSize = size * ATLAS_SCALE;
        Font awtFont = baseFont.deriveFont(Font.PLAIN, scaledSize);

        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D dg = dummy.createGraphics();
        applyHints(dg);
        dg.setFont(awtFont);
        FontMetrics fm = dg.getFontMetrics();
        FontRenderContext frc = dg.getFontRenderContext();
        dg.dispose();

        int pad        = Math.max(2, Math.round(2f * ATLAS_SCALE));
        int lineHeight = fm.getAscent() + fm.getDescent() + pad * 2;
        int spaceAdv   = fm.charWidth(' ') + 1;
        int lineHeightScaled = Math.max(1, Math.round(lineHeight / ATLAS_SCALE));
        int spaceAdvScaled   = Math.max(1, Math.round(spaceAdv / ATLAS_SCALE));

        char[] chars = CHARS.toCharArray();
        int cols  = 16;
        int rows  = (int) Math.ceil((double) chars.length / cols);
        int cellW = 0;
        for (char c : chars)
        {
            Rectangle2D bounds = awtFont.getStringBounds(String.valueOf(c), frc);
            cellW = Math.max(cellW, (int) Math.ceil(bounds.getWidth()) + pad * 2 + 2);
        }
        int cellH  = lineHeight;
        int atlasW = nextPow2(cols * cellW);
        int atlasH = nextPow2(rows * cellH);

        BufferedImage img = new BufferedImage(atlasW, atlasH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        applyHints(g2);
        g2.setFont(awtFont);
        g2.setColor(Color.WHITE);

        Map<Character, GlyphInfo> glyphs = new HashMap<>();

        for (int i = 0; i < chars.length; i++)
        {
            char ch  = chars[i];
            int col  = i % cols;
            int row  = i / cols;
            int gx   = col * cellW;
            int gy   = row * cellH;

            g2.drawString(String.valueOf(ch), gx + pad, gy + pad + fm.getAscent());

            GlyphInfo gi  = new GlyphInfo();
            gi.u0         = (float)  gx          / atlasW;
            gi.v0         = (float)  gy          / atlasH;
            gi.u1         = (float)(gx + cellW)  / atlasW;
            gi.v1         = (float)(gy + cellH)  / atlasH;
            gi.w          = Math.max(1, Math.round(cellW / ATLAS_SCALE));
            gi.h          = Math.max(1, Math.round(cellH / ATLAS_SCALE));
            gi.advance    = Math.max(1, Math.round((fm.charWidth(ch) + 1) / ATLAS_SCALE));
            glyphs.put(ch, gi);
        }
        g2.dispose();

        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        int[] px = img.getRGB(0, 0, atlasW, atlasH, null, 0, atlasW);
        ByteBuffer buf = ByteBuffer.allocateDirect(atlasW * atlasH * 4).order(ByteOrder.nativeOrder());
        for (int p : px)
        {
            buf.put((byte)((p >> 16) & 0xFF));
            buf.put((byte)((p >>  8) & 0xFF));
            buf.put((byte)( p        & 0xFF));
            buf.put((byte)((p >> 24) & 0xFF));
        }
        buf.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                atlasW, atlasH, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GlyphAtlas atlas   = new GlyphAtlas();
        atlas.textureId    = texId;
        atlas.glyphs       = glyphs;
        atlas.lineHeight   = lineHeightScaled;
        atlas.spaceAdvance = spaceAdvScaled;
        return atlas;
    }

    private void applyHints(Graphics2D g)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static int nextPow2(int n)
    {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    private void fallback(String text, float x, float y, int color)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, 0, 0, color);
        GlStateManager.popMatrix();
    }

    private static class GlyphAtlas
    {
        int textureId;
        Map<Character, GlyphInfo> glyphs;
        int lineHeight;
        int spaceAdvance;
    }

    private static class GlyphInfo
    {
        float u0, v0, u1, v1;
        int w, h, advance;
    }
}
