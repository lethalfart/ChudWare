package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.ActiveEventListener;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.Event;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import com.lethalfart.ChudWare.ui.modern.ModernClickGui;
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

public class AutoClickerHandler implements ActiveEventListener
{
    private static volatile long lastClickIntentAt = 0L;

    private final AutoClickerModule autoClickerModule;
    private final Method inventoryMouseClickedMethod;
    private final Random random = new Random();

    private boolean canClick = false;
    private int attackKey = -1;
    private long nextClickAt = 0L;
    private long nextInventoryClickAt = 0L;

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
            return;
        }

        if (mc.currentScreen instanceof ModernClickGui)
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

        if (handleBlockBreak(mc, mc.gameSettings.keyBindAttack.getKeyCode()))
        {
            canClick = false;
            attackKey = -1;
            nextClickAt = 0L;
            return;
        }

        if (autoClickerModule.isWeaponOnly() && !isHoldingWeapon(mc.thePlayer))
        {
            setCanClick(false, mc);
            return;
        }

        attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        canClick  = true;
        runClickScheduler(mc);
    }


    private void runClickScheduler(Minecraft mc)
    {
        if (mc == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        int key = attackKey;

        if (!canClick || key == -1 || now < nextClickAt)
        {
            return;
        }

        long baseTime = nextClickAt == 0L ? now : nextClickAt;
        long period = computePeriod(now, random);
        if (!hasFixedCps() && random.nextInt(100) < 5)
        {
            period += 80L + random.nextInt(120);
        }

        lastClickIntentAt = now;
        KeyBinding.setKeyBindState(key, true);
        KeyBinding.onTick(key);
        KeyBinding.setKeyBindState(key, false);
        nextClickAt = baseTime + period;
        if (nextClickAt <= now)
        {
            nextClickAt = now + 1L;
        }
    }

    private long computePeriod(long now, Random r)
    {
        double cps    = randomCps(r);
        long   period = Math.max(50L, Math.round(1000.0D / cps));

        if (hasFixedCps())
        {
            return period;
        }

        if (now > slowdownWindowUntil)
        {
            boolean roll       = r.nextInt(100) < 6;
            slowdownActive     = roll;
            slowdownMultiplier = roll ? (1.05D + r.nextDouble() * 0.08D) : 1.0D;
            slowdownWindowUntil = now + 1500L + r.nextInt(3000);
        }
        if (slowdownActive)
        {
            period = Math.max(50L, Math.round(period * slowdownMultiplier));
        }

        if (now > stallWindowUntil)
        {
            if (r.nextInt(100) < 8)
            {
                period += 30L + r.nextInt(60);
            }
            stallWindowUntil = now + 1500L + r.nextInt(3000);
        }

        return period;
    }

    private double randomCps(Random r)
    {
        float min = autoClickerModule.getMinCps();
        float max = autoClickerModule.getMaxCps();
        if (max < min) { float t = min; min = max; max = t; }

        double range = Math.max(0.0001D, max - min);
        if (r.nextInt(100) < 20) {
            return r.nextBoolean() ? min + (r.nextDouble() * range * 0.15D)
                    : max - (r.nextDouble() * range * 0.15D);
        }
        return min + (r.nextDouble() * range);
    }

    private boolean hasFixedCps()
    {
        return Math.abs(autoClickerModule.getMaxCps() - autoClickerModule.getMinCps()) < 0.001F;
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
        long now = System.currentTimeMillis();
        if (now < nextInventoryClickAt) return;

        clickInventory(mc.currentScreen, mc);
        nextInventoryClickAt = now + computePeriod(now, random);
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
            attackKey = -1;
            nextClickAt = 0L;
            nextInventoryClickAt = 0L;
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

    @Override
    public boolean shouldHandleEvent(Class<? extends Event> eventClass)
    {
        return eventClass == TickEvent.class && autoClickerModule != null && autoClickerModule.isEnabled();
    }

    private void resetInputState()
    {
        Minecraft mc = Minecraft.getMinecraft();
        setCanClick(false, mc);
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
