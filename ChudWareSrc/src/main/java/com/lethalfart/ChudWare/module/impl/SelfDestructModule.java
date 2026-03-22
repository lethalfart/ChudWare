package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import net.minecraft.client.Minecraft;

public class SelfDestructModule extends Module
{
    public static boolean selfDestructed;

    public SelfDestructModule()
    {
        super("Self Destruct", Category.MISC);
    }

    @Override
    protected void onEnable()
    {
        selfDestructed = true;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null)
        {
            mc.displayGuiScreen(null);
        }

        ChudWare.proxy.selfDestruct();
    }
}
