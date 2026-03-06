package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;

public class AutoClickerModule extends Module
{
    private float minCps = 12.0F;
    private float maxCps = 12.0F;

    public AutoClickerModule()
    {
        super("AutoClicker", Category.COMBAT);
    }

    public float getCps()
    {
        return maxCps < minCps ? maxCps : minCps;
    }

    public void setCps(float cps)
    {
        setMinCps(cps);
        setMaxCps(cps);
    }

    public float getMinCps()
    {
        return minCps;
    }

    public void setMinCps(float minCps)
    {
        this.minCps = sanitizeCps(minCps);
        if (this.minCps > this.maxCps)
        {
            this.maxCps = this.minCps;
        }
    }

    public float getMaxCps()
    {
        return maxCps;
    }

    public void setMaxCps(float maxCps)
    {
        this.maxCps = sanitizeCps(maxCps);
        if (this.maxCps < this.minCps)
        {
            this.minCps = this.maxCps;
        }
    }

    private static float sanitizeCps(float cps)
    {
        if (Float.isNaN(cps) || Float.isInfinite(cps))
        {
            cps = 12.0F;
        }
        return cps < 1.0F ? 1.0F : (cps > 20.0F ? 20.0F : cps);
    }
}
