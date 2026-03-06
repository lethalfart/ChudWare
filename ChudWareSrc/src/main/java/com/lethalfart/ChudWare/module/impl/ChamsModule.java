package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.util.ColorUtil;

public class ChamsModule extends Module
{
    private int visibleRed = 76;
    private int visibleGreen = 212;
    private int visibleBlue = 255;
    private int visibleAlpha = 186;

    private int hiddenRed = 255;
    private int hiddenGreen = 116;
    private int hiddenBlue = 168;
    private int hiddenAlpha = 154;

    public ChamsModule()
    {
        super("Chams", Category.VISUAL);
    }

    public int getVisibleRed()
    {
        return visibleRed;
    }

    public int getVisibleGreen()
    {
        return visibleGreen;
    }

    public int getVisibleBlue()
    {
        return visibleBlue;
    }

    public int getVisibleAlpha()
    {
        return visibleAlpha;
    }

    public int getHiddenRed()
    {
        return hiddenRed;
    }

    public int getHiddenGreen()
    {
        return hiddenGreen;
    }

    public int getHiddenBlue()
    {
        return hiddenBlue;
    }

    public int getHiddenAlpha()
    {
        return hiddenAlpha;
    }

    public void setVisibleRed(int value)
    {
        visibleRed = ColorUtil.clamp(value);
    }

    public void setVisibleGreen(int value)
    {
        visibleGreen = ColorUtil.clamp(value);
    }

    public void setVisibleBlue(int value)
    {
        visibleBlue = ColorUtil.clamp(value);
    }

    public void setVisibleAlpha(int value)
    {
        visibleAlpha = ColorUtil.clamp(value);
    }

    public void setHiddenRed(int value)
    {
        hiddenRed = ColorUtil.clamp(value);
    }

    public void setHiddenGreen(int value)
    {
        hiddenGreen = ColorUtil.clamp(value);
    }

    public void setHiddenBlue(int value)
    {
        hiddenBlue = ColorUtil.clamp(value);
    }

    public void setHiddenAlpha(int value)
    {
        hiddenAlpha = ColorUtil.clamp(value);
    }

    
}
