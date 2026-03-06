package com.lethalfart.ChudWare.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

public class FocusRecoveryHandler
{
    private boolean wasDisplayActive = true;
    private long nextRecoveryAllowedAtMs;
    private static final long RECOVERY_DELAY_MS = 160L;
    private static final long RECOVERY_COOLDOWN_MS = 500L;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        boolean displayActive = Display.isActive();

        if (!displayActive)
        {
            wasDisplayActive = false;
            return;
        }

        if (!wasDisplayActive)
        {
            wasDisplayActive = true;
            nextRecoveryAllowedAtMs = now + RECOVERY_DELAY_MS;
        }

        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            return;
        }

        if (!mc.inGameHasFocus && now >= nextRecoveryAllowedAtMs)
        {
            mc.setIngameFocus();
            nextRecoveryAllowedAtMs = now + RECOVERY_COOLDOWN_MS;
        }
    }
}
