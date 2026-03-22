package com.lethalfart.ChudWare.ui.modern;

public final class GuiEasing
{
    private GuiEasing() {}

    public static float clamp01(float t)
    {
        if (t < 0f) return 0f;
        if (t > 1f) return 1f;
        return t;
    }

    public static float easeOutCubic(float t)
    {
        t = clamp01(t);
        float u = 1f - t;
        return 1f - (u * u * u);
    }

    public static float easeOutQuint(float t)
    {
        t = clamp01(t);
        float u = 1f - t;
        return 1f - (u * u * u * u * u);
    }

    public static float easeInOutCubic(float t)
    {
        t = clamp01(t);
        if (t < 0.5f)
        {
            return 4f * t * t * t;
        }
        float u = -2f * t + 2f;
        return 1f - (u * u * u) / 2f;
    }
}
