package com.lethalfart.ChudWare.asm.hooks;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.impl.misc.PreMotionEvent;
import net.minecraft.client.Minecraft;

public class PreMotionHook
{
    public static void onPreMotion()
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;

            ChudWare.EVENT_MANAGER.call(PreMotionEvent.INSTANCE);
        }
        catch (Throwable ignored) {}
    }
}
