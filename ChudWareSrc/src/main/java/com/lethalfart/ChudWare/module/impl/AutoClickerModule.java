package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;

public class AutoClickerModule extends Module
{
    private float minCps = 12.0F;
    private float maxCps = 12.0F;
    private boolean inventoryFill = false;
    private boolean weaponOnly = false;
    private boolean breakBlocks = false;

    public AutoClickerModule()
    {
        super("AutoClicker", Category.COMBAT);
    }

    public float getMinCps()
    {
        return minCps;
    }

    public void setMinCps(float minCps)
    {
        this.minCps = sanitizeCps(minCps);
        if (this.minCps > this.maxCps)
            this.maxCps = this.minCps;
    }

    public float getMaxCps()
    {
        return maxCps;
    }

    public void setMaxCps(float maxCps)
    {
        this.maxCps = sanitizeCps(maxCps);
        if (this.maxCps < this.minCps)
            this.minCps = this.maxCps;
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

    public boolean isInventoryFill()
    {
        return inventoryFill;
    }

    public void setInventoryFill(boolean inventoryFill)
    {
        this.inventoryFill = inventoryFill;
    }

    public boolean isWeaponOnly()
    {
        return weaponOnly;
    }

    public void setWeaponOnly(boolean weaponOnly)
    {
        this.weaponOnly = weaponOnly;
    }

    public boolean isBreakBlocks()
    {
        return breakBlocks;
    }

    public void setBreakBlocks(boolean breakBlocks)
    {
        this.breakBlocks = breakBlocks;
    }

    private static float sanitizeCps(float cps)
    {
        if (Float.isNaN(cps) || Float.isInfinite(cps))
            cps = 12.0F;
        return Math.max(1.0F, Math.min(20.0F, cps));
    }
}
