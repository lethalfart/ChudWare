package com.lethalfart.ChudWare.asm.hooks;

import com.lethalfart.ChudWare.eventbus.impl.move.MoveEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.common.MinecraftForge;

public class MoveEntityHook
{
    public static double[] onMoveEntity(Object entity, double x, double y, double z)
    {
        if (!(entity instanceof EntityPlayerSP))
        {
            return new double[]{ x, y, z };
        }

        MoveEvent event = new MoveEvent(x, y, z);
        MinecraftForge.EVENT_BUS.post(event);
        return new double[]{ event.x, event.y, event.z };
    }
}
