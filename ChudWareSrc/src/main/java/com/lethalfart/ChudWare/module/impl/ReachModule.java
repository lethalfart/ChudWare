package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;

import java.util.Random;

public class ReachModule extends Module
{
    private static final float VANILLA_REACH = 3.0F;

    private float reachMin = 3.1F;
    private float reachMax = 3.3F;
    private final Random random = new Random();

    public ReachModule()
    {
        super("Reach", Category.COMBAT);
    }

    
    
    public double getReach()
    {
        double min = Math.min(reachMin, reachMax);
        double max = Math.max(reachMin, reachMax);
        double mid = (min + max) / 2.0;
        double range = (max - min) / 2.0;

        double value = mid + random.nextGaussian() * range * 0.4;
        return Math.max(min, Math.min(max, value));
    }

    public float getReachMin() { return reachMin; }
    public float getReachMax() { return reachMax; }

    public void setReachMin(float v)
    {
        reachMin = Math.max(VANILLA_REACH, v);
        if (reachMin > reachMax) reachMax = reachMin;
    }

    public void setReachMax(float v)
    {
        reachMax = Math.max(VANILLA_REACH, v);
        if (reachMax < reachMin) reachMin = reachMax;
    }
}