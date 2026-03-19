package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.util.ColorUtil;

public class ESPModule extends Module
{
    private int lineWidth = 3;
    private int maxDistance = 48;
    private int red = 84;
    private int green = 242;
    private int blue = 159;
    private int alpha = 220;
    private boolean showInvisible = false;

    public ESPModule()
    {
        super("ESP", Category.VISUAL);
    }

    public int getLineWidth()
    {
        return lineWidth;
    }

    public int getMaxDistance()
    {
        return maxDistance;
    }

    public int getRed()
    {
        return red;
    }

    public int getGreen()
    {
        return green;
    }

    public int getBlue()
    {
        return blue;
    }

    public int getAlpha()
    {
        return alpha;
    }

    public boolean isShowInvisible()
    {
        return showInvisible;
    }

    public void setShowInvisible(boolean showInvisible)
    {
        this.showInvisible = showInvisible;
    }

    public void setLineWidth(int lineWidth)
    {
        if (lineWidth < 1)
        {
            lineWidth = 1;
        }
        if (lineWidth > 10)
        {
            lineWidth = 10;
        }
        this.lineWidth = lineWidth;
    }

    public void setMaxDistance(int maxDistance)
    {
        if (maxDistance < 4)
        {
            maxDistance = 4;
        }
        if (maxDistance > 96)
        {
            maxDistance = 96;
        }
        this.maxDistance = maxDistance;
    }

    public void setRed(int red)
    {
        this.red = ColorUtil.clamp(red);
    }

    public void setGreen(int green)
    {
        this.green = ColorUtil.clamp(green);
    }

    public void setBlue(int blue)
    {
        this.blue = ColorUtil.clamp(blue);
    }

    public void setAlpha(int alpha)
    {
        this.alpha = ColorUtil.clamp(alpha);
    }
}
