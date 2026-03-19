package com.lethalfart.ChudWare.ui.modern;

public final class GuiAnim
{
    private GuiAnim() {}

    public static final class SmoothFloat
    {
        private float value;
        private float target;
        private float speed;

        public SmoothFloat(float value, float speed)
        {
            this.value = value;
            this.target = value;
            this.speed = Math.max(0.1f, speed);
        }

        public float get()
        {
            return value;
        }

        public void snap(float v)
        {
            value = v;
            target = v;
        }

        public void setTarget(float t)
        {
            target = t;
        }

        public void setSpeed(float speed)
        {
            this.speed = Math.max(0.1f, speed);
        }

        public void update(float dt)
        {
            if (dt <= 0f) return;
            float k = 1f - (float)Math.pow(2f, -speed * dt);
            value += (target - value) * k;
        }
    }

    public static final class TimedFloat
    {
        private float value;
        private float startValue;
        private float target;
        private long startTime;
        private long durationMs;
        private boolean active;

        public TimedFloat(float value)
        {
            this.value = value;
            this.target = value;
        }

        public float get()
        {
            return value;
        }

        public void snap(float v)
        {
            value = v;
            target = v;
            active = false;
        }

        public void animateTo(float target, long durationMs)
        {
            if (this.target == target && active)
            {
                return;
            }
            this.startValue = value;
            this.target = target;
            this.durationMs = Math.max(1L, durationMs);
            this.startTime = System.currentTimeMillis();
            this.active = true;
        }

        public void update(long now)
        {
            if (!active) return;
            float t = (now - startTime) / (float)durationMs;
            if (t >= 1f)
            {
                value = target;
                active = false;
                return;
            }
            float eased = GuiEasing.easeOutQuint(t);
            value = startValue + (target - startValue) * eased;
        }

        public boolean isActive()
        {
            return active;
        }
    }
}
