package com.lethalfart.ChudWare.ui.modern;

public class ScrollState
{
    private float value;
    private float target;
    private float max;
    private float speed = 18f;

    public float get()
    {
        return value;
    }

    public void setSpeed(float speed)
    {
        this.speed = Math.max(0.1f, speed);
    }

    public void setMax(float max)
    {
        this.max = Math.max(0f, max);
        if (target > this.max) target = this.max;
        if (value > this.max) value = this.max;
        if (target < 0f) target = 0f;
        if (value < 0f) value = 0f;
    }

    public void add(float delta)
    {
        target = clamp(target + delta, 0f, max);
    }

    public void update(float dt)
    {
        if (dt <= 0f) return;
        float k = 1f - (float)Math.pow(2f, -speed * dt);
        value += (target - value) * k;
    }

    private float clamp(float v, float min, float max)
    {
        return v < min ? min : (v > max ? max : v);
    }
}
