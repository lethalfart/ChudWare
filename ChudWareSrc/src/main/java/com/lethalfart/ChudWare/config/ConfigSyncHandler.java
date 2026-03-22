package com.lethalfart.ChudWare.config;

import com.lethalfart.ChudWare.module.ModuleManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class ConfigSyncHandler
{
    private final ConfigManager configManager;
    private final ModuleManager moduleManager;
    private long nextSaveAt;

    public ConfigSyncHandler(ConfigManager configManager, ModuleManager moduleManager)
    {
        this.configManager = configManager;
        this.moduleManager = moduleManager;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextSaveAt)
        {
            return;
        }

        configManager.save(moduleManager);
        nextSaveAt = now + 10000L;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        configManager.save(moduleManager);
    }
}
