package com.lethalfart.ChudWare.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FullBrightHandler
{
    private final FullBrightModule fullBrightModule;
    private boolean applied;
    private float previousGamma;

    public FullBrightHandler(FullBrightModule fullBrightModule)
    {
        this.fullBrightModule = fullBrightModule;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings == null)
        {
            return;
        }

        if (fullBrightModule.isEnabled())
        {
            if (!applied)
            {
                previousGamma = mc.gameSettings.gammaSetting;
                applied = true;
            }
            mc.gameSettings.gammaSetting = 100.0F;
            return;
        }

        if (applied)
        {
            mc.gameSettings.gammaSetting = previousGamma;
            applied = false;
        }
    }
}
