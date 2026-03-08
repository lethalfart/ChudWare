package com.lethalfart.ChudWare.module;

import com.lethalfart.ChudWare.module.impl.AutoClickerModule;
import com.lethalfart.ChudWare.module.impl.AimAssistModule;
import com.lethalfart.ChudWare.module.impl.ReachModule;
import com.lethalfart.ChudWare.module.impl.ArrayListModule;
import com.lethalfart.ChudWare.module.impl.ESPModule;
import com.lethalfart.ChudWare.module.impl.FullBrightModule;
import com.lethalfart.ChudWare.module.impl.ChamsModule;
import com.lethalfart.ChudWare.module.impl.AutoPotModule;
import com.lethalfart.ChudWare.module.impl.VelocityModule;

public final class ModuleRegistry
{
    private ModuleRegistry()
    {
    }

    public static void registerAll(ModuleManager moduleManager)
    {
        moduleManager.registerModule(new AutoClickerModule());
        moduleManager.registerModule(new AutoPotModule());
        moduleManager.registerModule(new ReachModule());
        moduleManager.registerModule(new VelocityModule());
        moduleManager.registerModule(new AimAssistModule());
        moduleManager.registerModule(new ESPModule());
        moduleManager.registerModule(new ChamsModule());
        moduleManager.registerModule(new FullBrightModule());
        moduleManager.registerModule(new ArrayListModule());
    }
}
