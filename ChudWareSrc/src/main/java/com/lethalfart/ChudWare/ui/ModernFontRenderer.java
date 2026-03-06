package com.lethalfart.ChudWare.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ModernFontRenderer
{
    private final FontRenderer fontRenderer;

    public ModernFontRenderer(Minecraft minecraft)
    {
        this.fontRenderer = minecraft.fontRendererObj;
    }

    public int drawString(String text, float x, float y, int color, boolean shadow, float scale)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        int out = shadow
                ? fontRenderer.drawStringWithShadow(text, 0.0F, 0.0F, color)
                : fontRenderer.drawString(text, 0.0F, 0.0F, color, false);
        GlStateManager.popMatrix();
        return out;
    }

    public int drawCenteredString(String text, float centerX, float y, int color, boolean shadow, float scale)
    {
        float x = centerX - (getStringWidth(text, scale) / 2.0F);
        return drawString(text, x, y, color, shadow, scale);
    }

    public int getStringWidth(String text, float scale)
    {
        return Math.round(fontRenderer.getStringWidth(text) * scale);
    }

    public int getFontHeight(float scale)
    {
        return Math.round(fontRenderer.FONT_HEIGHT * scale);
    }
}
