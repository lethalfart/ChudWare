package com.lethalfart.ChudWare;

import com.lethalfart.ChudWare.eventbus.EventManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = ChudWare.MODID, name = ChudWare.NAME, version = ChudWare.VERSION)
public class ChudWare
{
    public static final String MODID = "ChudWare";
    public static final String NAME = "ChudWare";
    public static final String VERSION = "1.0.5";
    public static final EventManager EVENT_MANAGER = new EventManager();

    @SidedProxy(clientSide = "com.lethalfart.ChudWare.ClientProxy", serverSide = "com.lethalfart.ChudWare.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init();
    }
}
