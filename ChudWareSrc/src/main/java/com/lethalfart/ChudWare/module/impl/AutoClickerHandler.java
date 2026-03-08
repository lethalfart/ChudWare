package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import com.lethalfart.ChudWare.ui.ModuleGuiScreen;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoClickerHandler
{
    private static volatile long lastClickIntentAt = 0L;

    private final AutoClickerModule autoClickerModule;
    private final Method inventoryMouseClickedMethod;

    private Thread clickThread;
    private final AtomicBoolean threadRunning = new AtomicBoolean(false);

    private volatile boolean canClick  = false;
    private volatile int     attackKey = -1;

    private long    slowdownWindowUntil = 0L;
    private long    stallWindowUntil    = 0L;
    private double  slowdownMultiplier  = 1.0D;
    private boolean slowdownActive      = false;

    private boolean breakHeld = false;

    public AutoClickerHandler(AutoClickerModule autoClickerModule)
    {
        this.autoClickerModule = autoClickerModule;
        this.inventoryMouseClickedMethod = resolveInventoryMouseClickedMethod();
        ChudWare.EVENT_MANAGER.register(this);
    }


    @EventTarget
    public void onTick(TickEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null)
        {
            setCanClick(false, mc);
            return;
        }

        if (!autoClickerModule.isEnabled())
        {
            setCanClick(false, mc);
            stopClickThread();
            return;
        }

        if (mc.currentScreen instanceof ModuleGuiScreen)
        {
            setCanClick(false, mc);
            return;
        }

        if (isInventoryContext(mc))
        {
            setCanClick(false, mc);
            doInventoryClick(mc);
            return;
        }

        if (!mc.inGameHasFocus || mc.currentScreen != null)
        {
            setCanClick(false, mc);
            return;
        }

        if (!Mouse.isButtonDown(0))
        {
            setCanClick(false, mc);
            return;
        }

        if (autoClickerModule.isWeaponOnly() && !isHoldingWeapon(mc.thePlayer))
        {
            setCanClick(false, mc);
            return;
        }

        if (handleBlockBreak(mc, mc.gameSettings.keyBindAttack.getKeyCode()))
        {
            canClick = false;
            return;
        }

        attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        canClick  = true;
        ensureClickThreadRunning();
    }


    private void ensureClickThreadRunning()
    {
        if (threadRunning.get()) return;
        threadRunning.set(true);
        clickThread = new Thread(this::clickLoop, "AutoClicker-Thread");
        clickThread.setDaemon(true);
        clickThread.start();
    }

    private void stopClickThread()
    {
        threadRunning.set(false);
        canClick = false;
        if (clickThread != null)
        {
            clickThread.interrupt();
            clickThread = null;
        }
    }

    private void clickLoop()
    {
        Random r = new Random();
        while (threadRunning.get())
        {
            if (!canClick)
            {
                sleepMs(5);
                continue;
            }

            long now    = System.currentTimeMillis();
            long period = computePeriod(now, r);
            int  key    = attackKey;

            if (key != -1 && canClick)
            {
                lastClickIntentAt = System.currentTimeMillis();
                KeyBinding.setKeyBindState(key, true);
                KeyBinding.onTick(key);
            }

            long holdMs = Math.max(15L, period / 3L) + (long)(r.nextGaussian() * 3.0);
            sleepMs(holdMs);

            if (key != -1)
            {
                KeyBinding.setKeyBindState(key, false);
            }

            long elapsed = System.currentTimeMillis() - now;
            long remain  = period - elapsed;
            if (remain > 0)
            {
                sleepMs(remain);
            }
        }
    }

    private long computePeriod(long now, Random r)
    {
        double cps    = randomCps(r);
        long   period = Math.max(50L, Math.round(1000.0D / cps));

        if (now > slowdownWindowUntil)
        {
            boolean roll       = r.nextInt(100) < 15;
            slowdownActive     = roll;
            slowdownMultiplier = roll ? (1.1D + r.nextDouble() * 0.15D) : 1.0D;
            slowdownWindowUntil = now + 500L + r.nextInt(1500);
        }
        if (slowdownActive)
        {
            period = Math.max(50L, Math.round(period * slowdownMultiplier));
        }

        if (now > stallWindowUntil)
        {
            if (r.nextInt(100) < 20)
            {
                period += 50L + r.nextInt(100);
            }
            stallWindowUntil = now + 500L + r.nextInt(1500);
        }

        return period;
    }

    private double randomCps(Random r)
    {
        float min = autoClickerModule.getMinCps();
        float max = autoClickerModule.getMaxCps();
        if (max < min) { float t = min; min = max; max = t; }

        double spanMin = Math.max(1.0D, min - 0.2D);
        double range   = Math.max(0.0001D, max - spanMin);
        return spanMin + (r.nextDouble() * range) + (0.3D * r.nextDouble());
    }

    private void sleepMs(long ms)
    {
        if (ms <= 0) return;
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }


    private boolean handleBlockBreak(Minecraft mc, int keyCode)
    {
        if (!autoClickerModule.isBreakBlocks() || mc.objectMouseOver == null)
        {
            clearBreakHeld(keyCode);
            return false;
        }

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null)
        {
            clearBreakHeld(keyCode);
            return false;
        }

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block != Blocks.air && !(block instanceof BlockLiquid))
        {
            if (!breakHeld)
            {
                KeyBinding.setKeyBindState(keyCode, true);
                KeyBinding.onTick(keyCode);
                breakHeld = true;
            }
            return true;
        }

        clearBreakHeld(keyCode);
        return false;
    }

    private void clearBreakHeld(int keyCode)
    {
        if (breakHeld)
        {
            KeyBinding.setKeyBindState(keyCode, false);
            breakHeld = false;
        }
    }


    private void doInventoryClick(Minecraft mc)
    {
        if (!autoClickerModule.isInventoryFill() || !isInventoryContext(mc)) return;

        boolean shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (!Mouse.isButtonDown(0) || !shiftHeld) return;
        if (inventoryMouseClickedMethod == null) return;

        clickInventory(mc.currentScreen, mc);
    }

    private void clickInventory(GuiScreen guiScreen, Minecraft mc)
    {
        if (guiScreen == null || mc == null) return;

        int mouseGuiX = Mouse.getX() * guiScreen.width  / mc.displayWidth;
        int mouseGuiY = guiScreen.height - Mouse.getY() * guiScreen.height / mc.displayHeight - 1;
        try
        {
            inventoryMouseClickedMethod.invoke(guiScreen, mouseGuiX, mouseGuiY, 0);
        }
        catch (IllegalAccessException | InvocationTargetException ignored) {}
    }


    private void setCanClick(boolean value, Minecraft mc)
    {
        canClick = value;
        if (!value)
        {
            if (mc != null)
            {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            }
            breakHeld = false;
        }
    }

    private boolean isInventoryContext(Minecraft mc)
    {
        return mc.currentScreen instanceof GuiInventory
                || mc.currentScreen instanceof GuiChest;
    }

    private boolean isHoldingWeapon(EntityPlayerSP player)
    {
        if (player == null || player.getHeldItem() == null) return false;
        return player.getHeldItem().getItem() instanceof ItemSword
                || player.getHeldItem().getItem() instanceof ItemAxe;
    }

    public static boolean hasRecentClickIntent(long windowMs)
    {
        return System.currentTimeMillis() - lastClickIntentAt <= windowMs;
    }

    private void resetInputState()
    {
        Minecraft mc = Minecraft.getMinecraft();
        setCanClick(false, mc);
        stopClickThread();
    }


    private Method resolveInventoryMouseClickedMethod()
    {
        try
        {
            Method method = ReflectionHelper.findMethod(
                    GuiScreen.class, null,
                    new String[]{"func_73864_a", "mouseClicked"},
                    Integer.TYPE, Integer.TYPE, Integer.TYPE);
            method.setAccessible(true);
            return method;
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }
}
