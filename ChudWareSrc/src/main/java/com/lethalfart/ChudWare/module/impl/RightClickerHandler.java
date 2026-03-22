package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.ActiveEventListener;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.Event;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import com.lethalfart.ChudWare.ui.modern.ModernClickGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.Random;

public class RightClickerHandler implements ActiveEventListener
{
    private final RightClickerModule rightClickerModule;
    private final Random random = new Random();

    private long nextClickAt;
    private long slowdownWindowUntil;
    private long stallWindowUntil;
    private double slowdownMultiplier = 1.0D;
    private boolean slowdownActive;

    public RightClickerHandler(RightClickerModule rightClickerModule)
    {
        this.rightClickerModule = rightClickerModule;
        ChudWare.EVENT_MANAGER.register(this);
    }

    @EventTarget
    public void onTick(TickEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null)
        {
            reset(mc);
            return;
        }

        if (!rightClickerModule.isEnabled())
        {
            reset(mc);
            return;
        }

        if (!mc.inGameHasFocus || mc.currentScreen != null || mc.currentScreen instanceof ModernClickGui)
        {
            reset(mc);
            return;
        }

        if (!Mouse.isButtonDown(1))
        {
            reset(mc);
            return;
        }
        if (!hasUseContext(mc))
        {
            reset(mc);
            return;
        }

        runClickScheduler(mc);
    }

    private void runClickScheduler(Minecraft mc)
    {
        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        long now = System.currentTimeMillis();
        if (key == -1 || now < nextClickAt)
        {
            return;
        }

        long baseTime = nextClickAt == 0L ? now : nextClickAt;
        long period = computePeriod(now);
        if (!hasFixedCps() && random.nextInt(100) < 5)
        {
            period += 80L + random.nextInt(120);
        }

        KeyBinding.onTick(key);
        nextClickAt = baseTime + period;
        if (nextClickAt <= now)
        {
            nextClickAt = now + 1L;
        }
    }

    private long computePeriod(long now)
    {
        double cps = randomCps();
        long period = Math.max(50L, Math.round(1000.0D / cps));

        if (hasFixedCps())
        {
            return period;
        }

        if (now > slowdownWindowUntil)
        {
            boolean roll = random.nextInt(100) < 6;
            slowdownActive = roll;
            slowdownMultiplier = roll ? (1.05D + random.nextDouble() * 0.08D) : 1.0D;
            slowdownWindowUntil = now + 1500L + random.nextInt(3000);
        }
        if (slowdownActive)
        {
            period = Math.max(50L, Math.round(period * slowdownMultiplier));
        }

        if (now > stallWindowUntil)
        {
            if (random.nextInt(100) < 8)
            {
                period += 30L + random.nextInt(60);
            }
            stallWindowUntil = now + 1500L + random.nextInt(3000);
        }

        return period;
    }

    private double randomCps()
    {
        float min = rightClickerModule.getMinCps();
        float max = rightClickerModule.getMaxCps();
        if (max < min)
        {
            float swap = min;
            min = max;
            max = swap;
        }

        double range = Math.max(0.0001D, max - min);
        if (random.nextInt(100) < 20)
        {
            return random.nextBoolean()
                    ? min + (random.nextDouble() * range * 0.15D)
                    : max - (random.nextDouble() * range * 0.15D);
        }
        return min + (random.nextDouble() * range);
    }

    private boolean hasFixedCps()
    {
        return Math.abs(rightClickerModule.getMaxCps() - rightClickerModule.getMinCps()) < 0.001F;
    }

    private void reset(Minecraft mc)
    {
        nextClickAt = 0L;
    }

    private boolean hasUseContext(Minecraft mc)
    {
        if (mc == null || mc.thePlayer == null)
        {
            return false;
        }
        if (mc.thePlayer.isUsingItem())
        {
            return true;
        }

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem != null)
        {
            return true;
        }

        return mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.MISS;
    }

    @Override
    public boolean shouldHandleEvent(Class<? extends Event> eventClass)
    {
        return eventClass == TickEvent.class && rightClickerModule != null && rightClickerModule.isEnabled();
    }
}
