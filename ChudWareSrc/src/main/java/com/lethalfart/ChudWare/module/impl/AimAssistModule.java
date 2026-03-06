package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;

public class AimAssistModule extends Module
{
    private double maxDistance = 4.2D;
    private int speed = 7;
    private int smoothness = 70;
    private boolean onClickOnly = true;

    public AimAssistModule()
    {
        super("AimAssist", Category.COMBAT);
    }

    public double getMaxDistance()
    {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance)
    {
        maxDistance = maxDistance < 1.0D ? 1.0D : (maxDistance > 6.0D ? 6.0D : maxDistance);
        this.maxDistance = maxDistance;
    }

    public int getSpeed()
    {
        return speed;
    }

    public void setSpeed(int speed)
    {
        speed = speed < 1 ? 1 : (speed > 20 ? 20 : speed);
        this.speed = speed;
    }

    public int getSmoothness()
    {
        return smoothness;
    }

    public void setSmoothness(int smoothness)
    {
        smoothness = smoothness < 1 ? 1 : (smoothness > 100 ? 100 : smoothness);
        this.smoothness = smoothness;
    }

    public boolean isOnClickOnly()
    {
        return onClickOnly;
    }

    public void setOnClickOnly(boolean onClickOnly)
    {
        this.onClickOnly = onClickOnly;
    }
}
