package com.lethalfart.ChudWare.asm.hooks;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.impl.misc.PreMotionEvent;
import com.lethalfart.ChudWare.eventbus.impl.misc.PreMotionAlwaysEvent;
import net.minecraft.client.Minecraft;

public class PreMotionHook
{
    public static void onPreMotion()
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            if (!ChudWare.EVENT_MANAGER.hasListeners(PreMotionEvent.class)) return;
            ChudWare.EVENT_MANAGER.call(PreMotionEvent.INSTANCE);
        }
        catch (Throwable ignored) {}
    }

    public static void onPreMotionAlways()
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            if (!ChudWare.EVENT_MANAGER.hasListeners(PreMotionAlwaysEvent.class)) return;
            ChudWare.EVENT_MANAGER.call(PreMotionAlwaysEvent.INSTANCE);
        }
        catch (Throwable ignored) {}
    }
}
