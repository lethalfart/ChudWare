package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.util.ColorUtil;

public class ChamsModule extends Module
{
    private static final float PULSE_MIN_ALPHA_RATIO = 0.35F;

    private int visibleRed = 138;
    private int visibleGreen = 138;
    private int visibleBlue = 255;
    private int visibleAlpha = 255;

    private int hiddenRed = 138;
    private int hiddenGreen = 138;
    private int hiddenBlue = 255;
    private int hiddenAlpha = 255;

    private float pulseSpeed = 10.0F;
    private boolean rainbow = false;
    private boolean colored = true;
    private boolean pulse = false;
    private boolean onlyTargets = false;
    private boolean material = false;

    private boolean targetPlayers = true;
    private boolean targetAnimals = false;
    private boolean targetMobs = false;
    private boolean targetPassives = false;

    private boolean pulseReversing;
    private float pulseAlpha;

    public ChamsModule()
    {
        super("Chams", Category.VISUAL);
    }

    public int getVisibleRed()
    {
        return visibleRed;
    }

    public void setVisibleRed(int value)
    {
        visibleRed = ColorUtil.clamp(value);
    }

    public int getVisibleGreen()
    {
        return visibleGreen;
    }

    public void setVisibleGreen(int value)
    {
        visibleGreen = ColorUtil.clamp(value);
    }

    public int getVisibleBlue()
    {
        return visibleBlue;
    }

    public void setVisibleBlue(int value)
    {
        visibleBlue = ColorUtil.clamp(value);
    }

    public int getVisibleAlpha()
    {
        return visibleAlpha;
    }

    public void setVisibleAlpha(int value)
    {
        visibleAlpha = clampRange(value, 50, 255);
        if (!pulse)
        {
            pulseAlpha = visibleAlpha;
        }
    }

    public int getHiddenRed()
    {
        return hiddenRed;
    }

    public void setHiddenRed(int value)
    {
        hiddenRed = ColorUtil.clamp(value);
    }

    public int getHiddenGreen()
    {
        return hiddenGreen;
    }

    public void setHiddenGreen(int value)
    {
        hiddenGreen = ColorUtil.clamp(value);
    }

    public int getHiddenBlue()
    {
        return hiddenBlue;
    }

    public void setHiddenBlue(int value)
    {
        hiddenBlue = ColorUtil.clamp(value);
    }

    public int getHiddenAlpha()
    {
        return hiddenAlpha;
    }

    public void setHiddenAlpha(int value)
    {
        hiddenAlpha = ColorUtil.clamp(value);
    }

    public float getPulseSpeed()
    {
        return pulseSpeed;
    }

    public void setPulseSpeed(float value)
    {
        pulseSpeed = clampFloat(value, 5.0F, 20.0F);
    }

    public boolean isRainbow()
    {
        return rainbow;
    }

    public void setRainbow(boolean rainbow)
    {
        this.rainbow = rainbow;
    }

    public boolean isColored()
    {
        return colored;
    }

    public void setColored(boolean colored)
    {
        this.colored = colored;
    }

    public boolean isPulse()
    {
        return pulse;
    }

    public void setPulse(boolean pulse)
    {
        this.pulse = pulse;
        if (!pulse)
        {
            pulseAlpha = visibleAlpha;
            pulseReversing = false;
        }
    }

    public boolean isOnlyTargets()
    {
        return onlyTargets;
    }

    public void setOnlyTargets(boolean onlyTargets)
    {
        this.onlyTargets = onlyTargets;
    }

    public boolean isMaterial()
    {
        return material;
    }

    public void setMaterial(boolean material)
    {
        this.material = material;
    }

    public boolean isTargetPlayers()
    {
        return targetPlayers;
    }

    public void setTargetPlayers(boolean targetPlayers)
    {
        this.targetPlayers = targetPlayers;
    }

    public boolean isTargetAnimals()
    {
        return targetAnimals;
    }

    public void setTargetAnimals(boolean targetAnimals)
    {
        this.targetAnimals = targetAnimals;
    }

    public boolean isTargetMobs()
    {
        return targetMobs;
    }

    public void setTargetMobs(boolean targetMobs)
    {
        this.targetMobs = targetMobs;
    }

    public boolean isTargetPassives()
    {
        return targetPassives;
    }

    public void setTargetPassives(boolean targetPassives)
    {
        this.targetPassives = targetPassives;
    }

    public int getRenderedVisibleAlpha()
    {
        return pulse ? Math.round(pulseAlpha) : visibleAlpha;
    }

    public void tickPulse()
    {
        if (!pulse)
        {
            pulseAlpha = visibleAlpha;
            return;
        }

        float minPulseAlpha = getMinPulseAlpha();
        pulseAlpha = clampFloat(pulseAlpha, minPulseAlpha, visibleAlpha);
        if (!pulseReversing && pulseAlpha < visibleAlpha)
        {
            pulseAlpha = clampFloat(pulseAlpha + pulseSpeed, minPulseAlpha, visibleAlpha);
        }
        else if (!pulseReversing && pulseAlpha >= visibleAlpha)
        {
            pulseReversing = true;
        }
        else if (pulseReversing && pulseAlpha > minPulseAlpha)
        {
            pulseAlpha = clampFloat(pulseAlpha - pulseSpeed, minPulseAlpha, visibleAlpha);
        }
        else
        {
            pulseReversing = false;
        }
    }

    @Override
    protected void onEnable()
    {
        pulseAlpha = visibleAlpha;
        pulseReversing = false;
    }

    private float getMinPulseAlpha()
    {
        return visibleAlpha * PULSE_MIN_ALPHA_RATIO;
    }

    private int clampRange(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    private float clampFloat(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
