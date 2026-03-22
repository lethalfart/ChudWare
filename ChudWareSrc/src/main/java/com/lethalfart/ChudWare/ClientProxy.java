package com.lethalfart.ChudWare;

import com.lethalfart.ChudWare.config.ConfigManager;
import com.lethalfart.ChudWare.config.ConfigSyncHandler;
import com.lethalfart.ChudWare.event.UpdateEventDispatcher;
import com.lethalfart.ChudWare.event.PacketEventBridge;
import com.lethalfart.ChudWare.module.ModuleManager;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.module.ModuleRegistry;
import com.lethalfart.ChudWare.module.impl.AutoClickerHandler;
import com.lethalfart.ChudWare.module.impl.AutoClickerModule;
import com.lethalfart.ChudWare.module.impl.AutoBridgeModule;
import com.lethalfart.ChudWare.module.impl.AimAssistHandler;
import com.lethalfart.ChudWare.module.impl.AimAssistModule;
import com.lethalfart.ChudWare.module.impl.OnePointSevenDelayHandler;
import com.lethalfart.ChudWare.module.impl.OnePointSevenDelayModule;
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
import com.lethalfart.ChudWare.module.impl.AutoPotModule;
import com.lethalfart.ChudWare.module.impl.RefillModule;
import com.lethalfart.ChudWare.module.impl.RightClickerHandler;
import com.lethalfart.ChudWare.module.impl.RightClickerModule;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.List;

public class ClientProxy extends CommonProxy
{
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private final List<Object> forgeRegistrations = new ArrayList<Object>();
    private final List<Object> fmlRegistrations = new ArrayList<Object>();
    private final List<Object> eventRegistrations = new ArrayList<Object>();
    private PacketEventBridge packetEventBridge;
    private boolean selfDestructed;

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
        registerFml(configSyncHandler);
        registerForge(configSyncHandler);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (configManager != null && moduleManager != null)
                {
                    configManager.saveBlocking(moduleManager);
                    configManager.flushPendingSaves();
                }
            }
        }, "ChudWare-ConfigSave"));

        registerFml(new UpdateEventDispatcher());
        packetEventBridge = new PacketEventBridge();
        registerFml(packetEventBridge);

        AutoClickerModule autoClickerModule = moduleManager.getModule(AutoClickerModule.class);
        if (autoClickerModule != null)
        {
            rememberEventRegistration(new AutoClickerHandler(autoClickerModule));
        }

        RightClickerModule rightClickerModule = moduleManager.getModule(RightClickerModule.class);
        if (rightClickerModule != null)
        {
            rememberEventRegistration(new RightClickerHandler(rightClickerModule));
        }

        AutoBridgeModule autoBridgeModule = moduleManager.getModule(AutoBridgeModule.class);
        if (autoBridgeModule != null)
        {
            registerFml(autoBridgeModule);
        }

        OnePointSevenDelayModule onePointSevenDelayModule = moduleManager.getModule(OnePointSevenDelayModule.class);
        if (onePointSevenDelayModule != null)
        {
            rememberEventRegistration(new OnePointSevenDelayHandler(onePointSevenDelayModule));
        }

        registerFml(new GuiToggleHandler(moduleManager, configManager));

        VelocityModule velocityModule = moduleManager.getModule(VelocityModule.class);
        if (velocityModule != null)
        {
            rememberEventRegistration(new VelocityHandler(velocityModule));
        }

        AimAssistModule aimAssistModule = moduleManager.getModule(AimAssistModule.class);
        if (aimAssistModule != null)
        {
            registerFml(new AimAssistHandler(aimAssistModule));
        }

        ESPModule espModule = moduleManager.getModule(ESPModule.class);
        if (espModule != null)
        {
            registerForge(new ESPHandler(espModule));
        }

        ChamsModule chamsModule = moduleManager.getModule(ChamsModule.class);
        if (chamsModule != null)
        {
            ChamsHandler chamsHandler = new ChamsHandler(chamsModule);
            registerForge(chamsHandler);
            registerFml(chamsHandler);
        }

        FullBrightModule fullBrightModule = moduleManager.getModule(FullBrightModule.class);
        if (fullBrightModule != null)
        {
            registerFml(new FullBrightHandler(fullBrightModule));
        }

        ArrayListModule arrayListModule = moduleManager.getModule(ArrayListModule.class);
        if (arrayListModule != null)
        {
            registerForge(new HudRenderer(moduleManager, arrayListModule));
        }

        AutoPotModule autoPotModule = moduleManager.getModule(AutoPotModule.class);
        if (autoPotModule != null)
        {
            rememberEventRegistration(autoPotModule.getHandler());
        }

        RefillModule refillModule = moduleManager.getModule(RefillModule.class);
        if (refillModule != null)
        {
            rememberEventRegistration(refillModule.getHandler());
        }
    }

    public ModuleManager getModuleManager()
    {
        return moduleManager;
    }

    @Override
    public void selfDestruct()
    {
        if (selfDestructed)
        {
            return;
        }
        selfDestructed = true;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null)
        {
            mc.displayGuiScreen(null);
        }

        if (moduleManager != null)
        {
            for (Module module : moduleManager.getModules())
            {
                module.setEnabled(false);
            }
        }

        if (packetEventBridge != null)
        {
            packetEventBridge.shutdown();
        }

        unregisterAll(MinecraftForge.EVENT_BUS, forgeRegistrations);
        unregisterAll(FMLCommonHandler.instance().bus(), fmlRegistrations);

        for (Object obj : eventRegistrations)
        {
            try
            {
                ChudWare.EVENT_MANAGER.unregister(obj);
            }
            catch (Exception ignored)
            {
            }
        }

        forgeRegistrations.clear();
        fmlRegistrations.clear();
        eventRegistrations.clear();
    }

    private void registerForge(Object obj)
    {
        MinecraftForge.EVENT_BUS.register(obj);
        forgeRegistrations.add(obj);
    }

    private void registerFml(Object obj)
    {
        FMLCommonHandler.instance().bus().register(obj);
        fmlRegistrations.add(obj);
    }

    private void rememberEventRegistration(Object obj)
    {
        if (obj != null)
        {
            eventRegistrations.add(obj);
        }
    }

    private void unregisterAll(net.minecraftforge.fml.common.eventhandler.EventBus bus, List<Object> registrations)
    {
        for (Object obj : registrations)
        {
            try
            {
                bus.unregister(obj);
            }
            catch (Exception ignored)
            {
            }
        }
    }
}
