package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.ActiveEventListener;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.Event;
import com.lethalfart.ChudWare.eventbus.impl.misc.PreMotionAlwaysEvent;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RefillHandler implements ActiveEventListener
{
    private static final int  HOTBAR_SIZE          = 9;
    private static final int  MAIN_INV_START       = 9;
    private static final int  MAIN_INV_END         = 35;
    private static final int  CONTAINER_HOTBAR     = 36;

    private static final long MIN_DELAY            = 12L;
    private static final long MIN_OPEN_DELAY       = 350L;
    private static final int  MIN_TICKS_AFTER_OPEN = 3;
    private static final long SERVER_CONFIRM       = 200L;

    private final RefillModule module;
    private final Minecraft    mc  = Minecraft.getMinecraft();
    private final Random       rng = new Random();

    private final RefillType[] lastSeen     = new RefillType[HOTBAR_SIZE];
    private final Set<Integer> clickedSlots = new HashSet<>();

    private Stage   stage          = Stage.IDLE;
    private boolean active         = false;
    private int     ticksSinceOpen = 0;
    private int     clickCount     = 0;

    private boolean pendingClick    = false;
    private int     pendingFromSlot = -1;
    private int     pendingWindowId = -1;

    private long openAt       = 0L;
    private long nextClickAt  = 0L;
    private long lastActionAt = 0L;
    private long closeAt      = 0L;

    public RefillHandler(RefillModule module)
    {
        this.module = module;
        ChudWare.EVENT_MANAGER.register(this);
    }

    public boolean isActive() { return active; }

    public void setActive(boolean active)
    {
        this.active = active;
        if (!active) resetFull();
    }

    @EventTarget
    public void onPreMotionAlways(PreMotionAlwaysEvent event)
    {
        if (!pendingClick) return;
        if (mc.thePlayer == null || mc.playerController == null)
        {
            clearPending();
            return;
        }

        Container container = mc.thePlayer.inventoryContainer;
        if (container == null || container.windowId != pendingWindowId)
        {
            clearPending();
            return;
        }

        if (pendingFromSlot < 0
                || pendingFromSlot >= container.inventorySlots.size()
                || container.getSlot(pendingFromSlot).getStack() == null)
        {
            clearPending();
            return;
        }

        if (mc.thePlayer.inventory.getItemStack() != null)
        {
            lastActionAt = System.currentTimeMillis();
            nextClickAt = lastActionAt + SERVER_CONFIRM;
            clearPending();
            return;
        }

        mc.playerController.windowClick(
                container.windowId, pendingFromSlot, 0, 1, mc.thePlayer
        );

        clearPending();
    }

    @EventTarget
    public void onTick(TickEvent event)
    {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null)
        {
            Arrays.fill(lastSeen, null);
            return;
        }

        updateLastSeen();
        if (!active) return;

        long now = System.currentTimeMillis();

        if (!(mc.currentScreen instanceof GuiInventory))
        {
            if (stage != Stage.IDLE)
            {
                resetSession();
            }
            return;
        }

        ticksSinceOpen++;

        switch (stage)
        {
            case IDLE:
            {
                if (!hasRefillCandidate()) return;
                openAt         = now;
                ticksSinceOpen = 0;
                stage          = Stage.WAIT_AFTER_OPEN;
                return;
            }

            case WAIT_AFTER_OPEN:
            {
                long required = Math.max(module.getDelayAfterOpen(), MIN_OPEN_DELAY);
                if (now - openAt < required) return;
                if (ticksSinceOpen < MIN_TICKS_AFTER_OPEN) return;
                stage = Stage.REFILLING;
                return;
            }

            case REFILLING:
            {
                if (now >= nextClickAt && !pendingClick) tickRefill(now);
                return;
            }

            case WAIT_BEFORE_CLOSE:
            {
                if (now >= closeAt) resetSession();
                return;
            }

            default: break;
        }
    }

    private void tickRefill(long now)
    {
        Container container = mc.thePlayer.inventoryContainer;
        if (container == null || mc.playerController == null) return;

        if (lastActionAt != 0L && now - lastActionAt < SERVER_CONFIRM) return;

        if (mc.thePlayer.inventory.getItemStack() != null)
        {
            nextClickAt = now + SERVER_CONFIRM;
            return;
        }

        int[] next = findNextAction();
        if (next == null)
        {
            stage   = Stage.WAIT_BEFORE_CLOSE;
            closeAt = now + humanDelay(module.getDelayBeforeClose(), 55L);
            return;
        }

        int fromSlot = invSlotToContainer(next[0]);
        if (fromSlot < 0) return;

        if (container.getSlot(fromSlot).getStack() == null)
        {
            clickedSlots.add(next[1]);
            nextClickAt = now + MIN_DELAY;
            return;
        }

        clickedSlots.add(next[1]);

        pendingClick    = true;
        pendingFromSlot = fromSlot;
        pendingWindowId = container.windowId;

        lastActionAt = now;
        nextClickAt  = now + interActionDelay();
    }

    private boolean hasRefillCandidate()
    {
        if (!module.isSoup() && !module.isPotion()) return false;
        for (int i = 0; i < HOTBAR_SIZE; i++)
        {
            if (clickedSlots.contains(i)) continue;
            if (mc.thePlayer.inventory.getStackInSlot(i) != null) continue;
            RefillType t = effectiveType(i);
            if (t != null && findInventorySlotIndex(t) != -1) return true;
        }
        return false;
    }

    private int[] findNextAction()
    {
        if (!module.isSoup() && !module.isPotion()) return null;
        for (int i = 0; i < HOTBAR_SIZE; i++)
        {
            if (clickedSlots.contains(i)) continue;
            if (mc.thePlayer.inventory.getStackInSlot(i) != null) continue;
            RefillType t = effectiveType(i);
            if (t == null) continue;
            int invSlot = findInventorySlotIndex(t);
            if (invSlot != -1) return new int[]{ invSlot, i };
        }
        return null;
    }

    private RefillType effectiveType(int hotbarIndex)
    {
        RefillType seen = lastSeen[hotbarIndex];
        if (seen != null)
        {
            return seen;
        }

        RefillType fallback = resolveDefaultType();
        if (fallback != null)
        {
            return fallback;
        }

        if (module.isSoup() && hasInventoryType(RefillType.SOUP))
        {
            return RefillType.SOUP;
        }

        if (module.isPotion() && hasInventoryType(RefillType.POTION))
        {
            return RefillType.POTION;
        }

        return null;
    }

    private RefillType resolveDefaultType()
    {
        if (module.isSoup() && !module.isPotion()) return RefillType.SOUP;
        if (module.isPotion() && !module.isSoup()) return RefillType.POTION;
        return null;
    }

    private boolean hasInventoryType(RefillType type)
    {
        for (int i = MAIN_INV_START; i <= MAIN_INV_END; i++)
        {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && classify(stack) == type)
            {
                return true;
            }
        }
        return false;
    }

    private int findInventorySlotIndex(RefillType type)
    {
        List<Integer> candidates = new ArrayList<>();
        for (int i = MAIN_INV_START; i <= MAIN_INV_END; i++)
        {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s != null && classify(s) == type) candidates.add(i);
        }
        if (candidates.isEmpty()) return -1;
        Collections.shuffle(candidates, rng);
        return candidates.get(0);
    }

    private int invSlotToContainer(int invIndex)
    {
        if (invIndex >= 0 && invIndex < HOTBAR_SIZE)
            return CONTAINER_HOTBAR + invIndex;
        if (invIndex >= MAIN_INV_START && invIndex <= MAIN_INV_END)
            return invIndex;
        return -1;
    }

    private RefillType classify(ItemStack stack)
    {
        if (stack == null) return null;
        if (module.isSoup() && stack.getItem() instanceof ItemSoup) return RefillType.SOUP;
        if (module.isPotion() && stack.getItem() instanceof ItemPotion)
        {
            if (!ItemPotion.isSplash(stack.getMetadata())) return null;
            if (module.isUseNonHealthPotions() || isHealthPotion(stack)) return RefillType.POTION;
        }
        return null;
    }

    private boolean isHealthPotion(ItemStack stack)
    {
        if (!(stack.getItem() instanceof ItemPotion)) return false;
        @SuppressWarnings("unchecked")
        List<PotionEffect> effects = ((ItemPotion) stack.getItem()).getEffects(stack);
        if (effects == null) return false;
        for (PotionEffect e : effects)
            if (e != null && e.getPotionID() == Potion.heal.id) return true;
        return false;
    }

    private long interActionDelay()
    {
        clickCount++;
        long base = module.isSmartSpeed()
                ? module.getBaseDelayMs()
                : Math.max(55L, module.getBaseDelayMs());

        double multiplier = 0.7 + (rng.nextDouble() * 0.9);
        long scaled = Math.round(base * multiplier);

        if (rng.nextInt(4) == 0)
        {
            scaled += humanDelay(120L, 80L);
        }

        return gaussianJitter(scaled, scaled * 0.25);
    }

    private long gaussianJitter(long base, double stddev)
    {
        return Math.max(MIN_DELAY, base + Math.round(rng.nextGaussian() * stddev));
    }

    private long humanDelay(long base, long spread)
    {
        return Math.max(MIN_DELAY, base + Math.round(rng.nextGaussian() * (spread / 2.0)));
    }

    private void updateLastSeen()
    {
        if (mc.thePlayer == null) return;
        for (int i = 0; i < HOTBAR_SIZE; i++)
        {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s != null)
            {
                lastSeen[i] = classify(s);
                clickedSlots.remove(i);
            }
        }
    }

    private void clearPending()
    {
        pendingClick    = false;
        pendingFromSlot = -1;
        pendingWindowId = -1;
    }

    private void resetSession()
    {
        stage          = Stage.IDLE;
        ticksSinceOpen = 0;
        clickCount     = 0;
        openAt         = 0L;
        nextClickAt    = 0L;
        lastActionAt   = 0L;
        closeAt        = 0L;
        clearPending();
        clickedSlots.clear();
    }

    private void resetFull()
    {
        resetSession();
        Arrays.fill(lastSeen, null);
    }

    private enum Stage
    {
        IDLE, WAIT_AFTER_OPEN, REFILLING, WAIT_BEFORE_CLOSE
    }

    private enum RefillType { SOUP, POTION }

    @Override
    public boolean shouldHandleEvent(Class<? extends Event> eventClass)
    {
        if (eventClass == TickEvent.class)
        {
            return active;
        }
        if (eventClass == PreMotionAlwaysEvent.class)
        {
            return pendingClick;
        }
        return false;
    }
}

//that was so hard to make nigger - lethalfart