package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.ActiveEventListener;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.Event;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class OnePointSevenDelayHandler implements ActiveEventListener
{
    private final OnePointSevenDelayModule onePointSevenDelayModule;
    private final Field leftClickCounterField;

    public OnePointSevenDelayHandler(OnePointSevenDelayModule onePointSevenDelayModule)
    {
        this.onePointSevenDelayModule = onePointSevenDelayModule;
        this.leftClickCounterField = resolveLeftClickCounterField();
        ChudWare.EVENT_MANAGER.register(this);
    }

    @EventTarget
    public void onTick(TickEvent event)
    {
        if (!onePointSevenDelayModule.isEnabled() || leftClickCounterField == null)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }
        if (!mc.inGameHasFocus || mc.thePlayer.capabilities.isCreativeMode)
        {
            return;
        }
        if (!hasAttackContext(mc))
        {
            return;
        }

        try
        {
            leftClickCounterField.setInt(mc, 0);
        }
        catch (IllegalAccessException ignored)
        {
        }
        catch (IndexOutOfBoundsException ignored)
        {
        }
    }

    private Field resolveLeftClickCounterField()
    {
        try
        {
            Field field = ReflectionHelper.findField(Minecraft.class, "field_71429_W", "leftClickCounter");
            field.setAccessible(true);
            return field;
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }

    private boolean hasAttackContext(Minecraft mc)
    {
        if (!Mouse.isButtonDown(0) && !AutoClickerHandler.hasRecentClickIntent(150L))
        {
            return false;
        }
        if (mc.objectMouseOver == null)
        {
            return false;
        }
        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)
        {
            return mc.objectMouseOver.entityHit != null;
        }
        if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
        {
            return false;
        }

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null || mc.theWorld == null)
        {
            return false;
        }

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return !(block instanceof BlockAir) && !(block instanceof BlockLiquid);
    }

    @Override
    public boolean shouldHandleEvent(Class<? extends Event> eventClass)
    {
        return eventClass == TickEvent.class && onePointSevenDelayModule != null && onePointSevenDelayModule.isEnabled();
    }
}
