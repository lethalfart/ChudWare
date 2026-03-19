package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

public class OnePointSevenDelayHandler
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
}
