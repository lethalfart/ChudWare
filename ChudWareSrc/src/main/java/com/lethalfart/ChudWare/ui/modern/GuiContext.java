package com.lethalfart.ChudWare.ui.modern;

import com.lethalfart.ChudWare.ui.ModernFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class GuiContext
{
    public final Minecraft mc;
    public final ModernFontRenderer font;
    public final ScaledResolution sr;
    public final int screenW;
    public final int screenH;
    public final int mouseX;
    public final int mouseY;
    public final long now;
    public final float delta;

    public GuiContext(Minecraft mc, ModernFontRenderer font, ScaledResolution sr,
                      int mouseX, int mouseY, long now, float delta)
    {
        this.mc = mc;
        this.font = font;
        this.sr = sr;
        this.screenW = sr.getScaledWidth();
        this.screenH = sr.getScaledHeight();
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.now = now;
        this.delta = delta;
    }

    public boolean hit(int x, int y, int w, int h)
    {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
