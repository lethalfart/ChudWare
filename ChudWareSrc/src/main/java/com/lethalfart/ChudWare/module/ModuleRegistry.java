package com.lethalfart.ChudWare.module;

import com.lethalfart.ChudWare.module.impl.AutoClickerModule;
import com.lethalfart.ChudWare.module.impl.AimAssistModule;
import com.lethalfart.ChudWare.module.impl.AutoBridgeModule;
import com.lethalfart.ChudWare.module.impl.ArrayListModule;
import com.lethalfart.ChudWare.module.impl.ESPModule;
import com.lethalfart.ChudWare.module.impl.FullBrightModule;
import com.lethalfart.ChudWare.module.impl.ChamsModule;
import com.lethalfart.ChudWare.module.impl.AutoPotModule;
import com.lethalfart.ChudWare.module.impl.OnePointSevenDelayModule;
import com.lethalfart.ChudWare.module.impl.VelocityModule;
import com.lethalfart.ChudWare.module.impl.RefillModule;
import com.lethalfart.ChudWare.module.impl.RightClickerModule;
import com.lethalfart.ChudWare.module.impl.SelfDestructModule;

public final class ModuleRegistry
{
    private ModuleRegistry()
    {
    }

    public static void registerAll(ModuleManager moduleManager)
    {
        moduleManager.registerModule(new AutoClickerModule());
        moduleManager.registerModule(new AutoBridgeModule());
        moduleManager.registerModule(new OnePointSevenDelayModule());
        moduleManager.registerModule(new AutoPotModule());
        moduleManager.registerModule(new VelocityModule());
        moduleManager.registerModule(new RefillModule());
        moduleManager.registerModule(new RightClickerModule());
        moduleManager.registerModule(new AimAssistModule());
        moduleManager.registerModule(new ESPModule());
        moduleManager.registerModule(new ChamsModule());
        moduleManager.registerModule(new FullBrightModule());
        moduleManager.registerModule(new ArrayListModule());
        moduleManager.registerModule(new SelfDestructModule());
    }
}
