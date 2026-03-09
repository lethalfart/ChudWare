package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

import java.util.List;

public class AutoPotHandler
{
    private static final int HOTBAR_SLOTS = 9;
    private static final int STAGE_IDLE = 0;
    private static final int STAGE_THROW = 1;
    private static final int STAGE_RETURN = 2;
    private static final int RETURN_SLOT = 0;
    private final AutoPotModule autoPotModule;
    private final Minecraft mc = Minecraft.getMinecraft();

    private int stage = STAGE_IDLE;
    private long cooldownUntil = 0L;

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

        if (!triggerHotbarSlot(slot))
        {
            reset();
            return;
        }

        stage = STAGE_THROW;
    }

    @EventTarget
    public void onTick(TickEvent event)
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            reset();
            return;
        }
        if (mc.currentScreen != null)
        {
            reset();
            return;
        }

        if (stage == STAGE_IDLE)
        {
            return;
        }

        if (!canContinueAutoPot())
        {
            reset();
            return;
        }

        if (stage == STAGE_THROW)
        {
            if (triggerUseItem())
            {
                stage = STAGE_RETURN;
            }
            return;
        }

        if (stage == STAGE_RETURN)
        {
            triggerHotbarSlot(RETURN_SLOT);
            cooldownUntil = System.currentTimeMillis() + autoPotModule.getActionDelay();
            reset();
            return;
        }

        reset();
    }

    public boolean isExecuting()
    {
        return stage != STAGE_IDLE;
    }

    private void reset()
    {
        stage = STAGE_IDLE;
    }

    private boolean canContinueAutoPot()
    {
        return !mc.thePlayer.isUsingItem();
    }

    private int resolvePotionSlot()
    {
        for (int slot = 0; slot < HOTBAR_SLOTS; slot++)
        {
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
