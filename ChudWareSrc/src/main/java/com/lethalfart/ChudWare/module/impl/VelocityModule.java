package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import net.minecraft.util.MathHelper;

public class VelocityModule extends Module
{
    private boolean noWater = true;
    private boolean noLava = true;
    private boolean noLadder = true;
    private int chance = 100;
    private double horizontalPercent = 80.0D;
    private double verticalPercent = 100.0D;
    private boolean bufferMode = false;
    private int bufferTicks = 2;

    public VelocityModule()
    {
        super("Velocity", Category.MOVEMENT);
    }

    public boolean isNoWater() { return noWater; }
    public void setNoWater(boolean noWater) { this.noWater = noWater; }

    public boolean isNoLava() { return noLava; }
    public void setNoLava(boolean noLava) { this.noLava = noLava; }

    public boolean isNoLadder() { return noLadder; }
    public void setNoLadder(boolean noLadder) { this.noLadder = noLadder; }

    public int getChance() { return chance; }
    public void setChance(int chance)
    {
        this.chance = MathHelper.clamp_int(chance, 10, 100);
    }

    public double getHorizontalPercent() { return horizontalPercent; }
    public void setHorizontalPercent(double horizontalPercent)
    {
        this.horizontalPercent = MathHelper.clamp_double(horizontalPercent, 0.0D, 100.0D);
    }

    public double getVerticalPercent() { return verticalPercent; }
    public void setVerticalPercent(double verticalPercent)
    {
        this.verticalPercent = MathHelper.clamp_double(verticalPercent, 0.0D, 100.0D);
    }

    public boolean isBufferMode() { return bufferMode; }
    public void setBufferMode(boolean bufferMode) { this.bufferMode = bufferMode; }

    public int getBufferTicks() { return bufferTicks; }
    public void setBufferTicks(int bufferTicks)
    {
        this.bufferTicks = MathHelper.clamp_int(bufferTicks, 1, 10);
    }
}
