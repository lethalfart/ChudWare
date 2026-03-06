package com.lethalfart.ChudWare;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.config.ConfigSyncHandler;
import com.lethalfart.ChudWare.event.UpdateEventDispatcher;
import com.lethalfart.ChudWare.event.PacketEventBridge;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.ModuleRegistry;
import com.lethalfart.ChudWare.module.impl.AutoClickerHandler;
import com.lethalfart.ChudWare.module.impl.AutoClickerModule;
import com.lethalfart.ChudWare.module.impl.AimAssistHandler;
import com.lethalfart.ChudWare.module.impl.AimAssistModule;
import com.lethalfart.ChudWare.module.impl.ReachHandler;
import com.lethalfart.ChudWare.module.impl.ReachModule;
import com.lethalfart.ChudWare.module.impl.VelocityHandler;
import com.lethalfart.ChudWare.module.impl.VelocityModule;
import com.lethalfart.ChudWare.ui.GuiToggleHandler;
import com.lethalfart.ChudWare.module.impl.ArrayListModule;
import com.lethalfart.ChudWare.ui.HudRenderer;
import com.lethalfart.ChudWare.module.impl.ESPHandler;
import com.lethalfart.ChudWare.module.impl.ESPModule;
import com.lethalfart.ChudWare.module.impl.ChamsHandler;
import com.lethalfart.ChudWare.module.impl.ChamsModule;
import com.lethalfart.ChudWare.module.impl.FullBrightHandler;
import com.lethalfart.ChudWare.module.impl.FullBrightModule;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import com.lethalfart.ChudWare.util.FocusRecoveryHandler;

public class ClientProxy extends CommonProxy
{
    private ModuleManager moduleManager;
    private ConfigManager configManager;

    @Override
    public void preInit()
    {
        moduleManager = new ModuleManager();
        ModuleRegistry.registerAll(moduleManager);
        configManager = new ConfigManager();
        configManager.load(moduleManager);
    }

    @Override
    public void init()
    {
        ConfigSyncHandler configSyncHandler = new ConfigSyncHandler(configManager, moduleManager);
        FMLCommonHandler.instance().bus().register(configSyncHandler);
        MinecraftForge.EVENT_BUS.register(configSyncHandler);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (configManager != null && moduleManager != null)
                {
                    configManager.save(moduleManager);
                }
            }
        }, "ChudWare-ConfigSave"));

        FMLCommonHandler.instance().bus().register(new GuiToggleHandler(moduleManager, configManager));
        FMLCommonHandler.instance().bus().register(new UpdateEventDispatcher());
        FMLCommonHandler.instance().bus().register(new FocusRecoveryHandler());
        FMLCommonHandler.instance().bus().register(new PacketEventBridge());

        AutoClickerModule autoClickerModule = moduleManager.getModule(AutoClickerModule.class);
        if (autoClickerModule != null)
        {
            new AutoClickerHandler(autoClickerModule);
        }

        ReachModule reachModule = moduleManager.getModule(ReachModule.class);
        if (reachModule != null)
        {
            new ReachHandler(reachModule);
        }

        VelocityModule velocityModule = moduleManager.getModule(VelocityModule.class);
        if (velocityModule != null)
        {
            new VelocityHandler(velocityModule);
        }

        AimAssistModule aimAssistModule = moduleManager.getModule(AimAssistModule.class);
        if (aimAssistModule != null)
        {
            FMLCommonHandler.instance().bus().register(new AimAssistHandler(aimAssistModule));
        }

        ESPModule espModule = moduleManager.getModule(ESPModule.class);
        if (espModule != null)
        {
            MinecraftForge.EVENT_BUS.register(new ESPHandler(espModule));
        }

        ChamsModule chamsModule = moduleManager.getModule(ChamsModule.class);
        if (chamsModule != null)
        {
            MinecraftForge.EVENT_BUS.register(new ChamsHandler(chamsModule));
        }

        FullBrightModule fullBrightModule = moduleManager.getModule(FullBrightModule.class);
        if (fullBrightModule != null)
        {
            FMLCommonHandler.instance().bus().register(new FullBrightHandler(fullBrightModule));
        }

        ArrayListModule arrayListModule = moduleManager.getModule(ArrayListModule.class);
        if (arrayListModule != null)
        {
            MinecraftForge.EVENT_BUS.register(new HudRenderer(moduleManager, arrayListModule));
        }
    }

    public ModuleManager getModuleManager()
    {
        return moduleManager;
    }
}