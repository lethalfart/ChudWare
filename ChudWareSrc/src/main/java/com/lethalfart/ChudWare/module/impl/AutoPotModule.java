package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import net.minecraft.util.MathHelper;

public class AutoPotModule extends Module
{
    private static final int HOTBAR_SLOTS = 9;

    private int actionDelay = 90;
    private final boolean[] enabledSlots = new boolean[HOTBAR_SLOTS];
    private final AutoPotHandler handler;

    public AutoPotModule()
    {
        super("AutoPot", Category.COMBAT);
        for (int i = 0; i < HOTBAR_SLOTS; i++)
        {
            enabledSlots[i] = true;
        }
        handler = new AutoPotHandler(this);
    }

    public int getActionDelay()
    {
        return actionDelay;
    }

    public void setActionDelay(int actionDelay)
    {
        this.actionDelay = MathHelper.clamp_int(actionDelay, 20, 500);
    }

    public boolean isSlotEnabled(int slot)
    {
        return slot >= 0 && slot < HOTBAR_SLOTS && enabledSlots[slot];
    }

    public void setSlotEnabled(int slot, boolean enabled)
    {
        if (slot < 0 || slot >= HOTBAR_SLOTS)
        {
            return;
        }
        enabledSlots[slot] = enabled;
    }

    public int getHotbarSlotCount()
    {
        return HOTBAR_SLOTS;
    }

    public AutoPotHandler getHandler()
    {
        return handler;
    }

    @Override
    public void onKeyPress()
    {
        handler.onKeyPress();
    }

    @Override
    public void toggle()
    {
        setEnabled(!isEnabled());
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
    }
}
