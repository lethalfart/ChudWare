package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.util.List;

public class AutoPotHandler
{
    private static final int HOTBAR_SLOTS = 9;
    private static final int STAGE_IDLE = 0;
    private static final int STAGE_SWITCH = 1;
    private static final int STAGE_LOOK = 2;
    private static final int STAGE_THROW = 3;
    private static final int STAGE_RESTORE = 4;
    private static final float THROW_PITCH = 90.0F;
    private static final long SLOT_SWITCH_DELAY_MS = 85L;
    private static final long LOOK_STEP_DELAY_MS = 7L;
    private static final long THROW_DELAY_MS = 42L;
    private static final long RESTORE_STEP_DELAY_MS = 7L;
    private static final float MIN_PITCH_STEP = 11.0F;
    private static final float MAX_PITCH_STEP = 20.0F;
    private final AutoPotModule autoPotModule;
    private final Minecraft mc = Minecraft.getMinecraft();

    private int stage = STAGE_IDLE;
    private int pendingSlot = -1;
    private float previousPitch = 0.0F;
    private long cooldownUntil = 0L;
    private long nextStageAt = 0L;

    public AutoPotHandler(AutoPotModule autoPotModule)
    {
        this.autoPotModule = autoPotModule;
        ChudWare.EVENT_MANAGER.register(this);
    }

    public void onKeyPress()
    {
        if (stage != STAGE_IDLE)
        {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            return;
        }
        if (System.currentTimeMillis() < cooldownUntil)
        {
            return;
        }
        if (!canContinueAutoPot())
        {
            return;
        }

        int slot = resolvePotionSlot();
        if (slot == -1)
        {
            return;
        }

        pendingSlot = slot;
        previousPitch = mc.thePlayer.rotationPitch;
        stage = STAGE_SWITCH;
        nextStageAt = System.currentTimeMillis() + SLOT_SWITCH_DELAY_MS;
    }

    @EventTarget
    public void onTick(TickEvent event)
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            reset(false);
            return;
        }
        if (mc.currentScreen != null)
        {
            reset(true);
            return;
        }

        long now = System.currentTimeMillis();
        if (stage == STAGE_IDLE || now < nextStageAt)
        {
            return;
        }

        switch (stage)
        {
            case STAGE_SWITCH:
                if (!canContinueAutoPot())
                {
                    reset(true);
                    return;
                }
                if (!triggerHotbarSlot(pendingSlot))
                {
                    reset(true);
                    return;
                }
                stage = STAGE_LOOK;
                nextStageAt = now + LOOK_STEP_DELAY_MS;
                break;

            case STAGE_LOOK:
                if (!canContinueAutoPot())
                {
                    reset(true);
                    return;
                }
                mc.thePlayer.rotationPitch = stepPitchTowards(mc.thePlayer.rotationPitch, THROW_PITCH);
                if (Math.abs(THROW_PITCH - mc.thePlayer.rotationPitch) <= 0.5F)
                {
                    mc.thePlayer.rotationPitch = THROW_PITCH;
                    stage = STAGE_THROW;
                    nextStageAt = now + THROW_DELAY_MS;
                }
                else
                {
                    nextStageAt = now + LOOK_STEP_DELAY_MS;
                }
                break;

            case STAGE_THROW:
                if (!canContinueAutoPot())
                {
                    reset(true);
                    return;
                }
                mc.thePlayer.rotationPitch = THROW_PITCH;
                if (triggerUseItem())
                {
                    stage = STAGE_RESTORE;
                    nextStageAt = now + RESTORE_STEP_DELAY_MS;
                }
                else
                {
                    reset(true);
                }
                break;

            case STAGE_RESTORE:
                mc.thePlayer.rotationPitch = stepPitchTowards(mc.thePlayer.rotationPitch, previousPitch);
                if (Math.abs(previousPitch - mc.thePlayer.rotationPitch) <= 0.5F)
                {
                    mc.thePlayer.rotationPitch = previousPitch;
                    cooldownUntil = now + autoPotModule.getActionDelay();
                    stage = STAGE_IDLE;
                    pendingSlot = -1;
                    nextStageAt = 0L;
                }
                else
                {
                    nextStageAt = now + RESTORE_STEP_DELAY_MS;
                }
                break;

            default:
                reset(true);
                break;
        }
    }

    public boolean isExecuting()
    {
        return stage != STAGE_IDLE;
    }

    private void restorePreviousState()
    {
        mc.thePlayer.rotationPitch = previousPitch;
    }

    private float stepPitchTowards(float currentPitch, float targetPitch)
    {
        float delta = MathHelper.wrapAngleTo180_float(targetPitch - currentPitch);
        float distance = Math.abs(delta);
        if (distance <= MIN_PITCH_STEP)
        {
            return targetPitch;
        }

        float step = Math.min(MAX_PITCH_STEP, Math.max(MIN_PITCH_STEP, distance * 0.72F));
        return currentPitch + (delta > 0.0F ? step : -step);
    }

    private void reset(boolean restoreState)
    {
        if (restoreState && mc.thePlayer != null)
        {
            restorePreviousState();
        }
        stage = STAGE_IDLE;
        pendingSlot = -1;
        nextStageAt = 0L;
    }

    private boolean canContinueAutoPot()
    {
        if (mc.thePlayer.isUsingItem())
        {
            return false;
        }
        return hasGroundBelow(2.0D);
    }

    private boolean hasGroundBelow(double maxDistance)
    {
        double step = 0.2D;
        for (double offset = 0.0D; offset <= maxDistance; offset += step)
        {
            BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - offset, mc.thePlayer.posZ);
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block != null && block.isFullBlock())
            {
                return true;
            }
        }
        return false;
    }

    private int resolvePotionSlot()
    {
        for (int slot = 0; slot < HOTBAR_SLOTS; slot++)
        {
            if (!autoPotModule.isSlotEnabled(slot))
            {
                continue;
            }
            if (isEligiblePotion(slot))
            {
                return slot;
            }
        }
        return -1;
    }

    private boolean isEligiblePotion(int slot)
    {
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if (stack == null || !(stack.getItem() instanceof ItemPotion) || !ItemPotion.isSplash(stack.getMetadata()))
        {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<PotionEffect> effects = ((ItemPotion) stack.getItem()).getEffects(stack);
        if (effects == null)
        {
            return false;
        }

        for (PotionEffect effect : effects)
        {
            int potionId = effect.getPotionID();
            if (potionId < 0 || potionId >= net.minecraft.potion.Potion.potionTypes.length)
            {
                continue;
            }
            net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.potionTypes[potionId];
            if (potion == null || potion.isBadEffect())
            {
                continue;
            }

            return true;
        }
        return false;
    }

    private boolean triggerHotbarSlot(int slot)
    {
        if (slot < 0 || slot >= HOTBAR_SLOTS)
        {
            return false;
        }

        KeyBinding[] hotbarKeys = mc.gameSettings.keyBindsHotbar;
        if (hotbarKeys == null || slot >= hotbarKeys.length || hotbarKeys[slot] == null)
        {
            return false;
        }

        int keyCode = hotbarKeys[slot].getKeyCode();
        KeyBinding.setKeyBindState(keyCode, true);
        KeyBinding.onTick(keyCode);
        KeyBinding.setKeyBindState(keyCode, false);
        return true;
    }

    private boolean triggerUseItem()
    {
        if (mc.gameSettings == null || mc.gameSettings.keyBindUseItem == null)
        {
            return false;
        }

        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(keyCode, true);
        KeyBinding.onTick(keyCode);
        KeyBinding.setKeyBindState(keyCode, false);
        return true;
    }
}
