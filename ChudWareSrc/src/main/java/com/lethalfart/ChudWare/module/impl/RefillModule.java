package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import net.minecraft.util.MathHelper;

public class RefillModule extends Module
{
    public static final int BUTTON_LEFT  = 0;
    public static final int BUTTON_RIGHT = 1;
    public static final int BUTTON_BOTH  = 2;

    private boolean soup                = true;
    private boolean potion              = true;
    private boolean useNonHealthPotions = false;
    private int     simulateButton      = BUTTON_LEFT;
    private int     delayAfterOpen      = 120;
    private int     delayBeforeClose    = 120;
    private float   speed               = 8.0f;
    private boolean smartSpeed          = true;

    private final RefillHandler handler;

    public RefillModule()
    {
        super("Refill", Category.MISC);
        handler = new RefillHandler(this);
    }

    @Override
    public void onKeyPress()
    {
        boolean newState = !isEnabled();
        super.setEnabled(newState);
        handler.setActive(newState);
    }

    @Override
    public void toggle()
    {
        boolean newState = !isEnabled();
        super.setEnabled(newState);
        handler.setActive(newState);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        handler.setActive(enabled);
    }

    public boolean isSoup() { return soup; }
    public void setSoup(boolean soup) { this.soup = soup; }

    public boolean isPotion() { return potion; }
    public void setPotion(boolean potion) { this.potion = potion; }

    public boolean isUseNonHealthPotions() { return useNonHealthPotions; }
    public void setUseNonHealthPotions(boolean v) { this.useNonHealthPotions = v; }

    public int getSimulateButton() { return simulateButton; }
    public void setSimulateButton(int v)
    {
        this.simulateButton = MathHelper.clamp_int(v, BUTTON_LEFT, BUTTON_BOTH);
    }

    public int getDelayAfterOpen() { return delayAfterOpen; }
    public void setDelayAfterOpen(int v)
    {
        this.delayAfterOpen = MathHelper.clamp_int(v, 0, 1000);
    }

    public int getDelayBeforeClose() { return delayBeforeClose; }
    public void setDelayBeforeClose(int v)
    {
        this.delayBeforeClose = MathHelper.clamp_int(v, 0, 1000);
    }

    public float getSpeed() { return speed; }
    public void setSpeed(float v)
    {
        this.speed = MathHelper.clamp_float(v, 1.0f, 20.0f);
    }

    public boolean isSmartSpeed() { return smartSpeed; }
    public void setSmartSpeed(boolean v) { this.smartSpeed = v; }

    public long getBaseDelayMs()
    {
        float delay = 1000.0f / Math.max(0.1f, speed);
        return Math.round(MathHelper.clamp_float(delay, 40.0f, 1000.0f));
    }
}
