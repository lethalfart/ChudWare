package com.lethalfart.ChudWare.event;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.impl.player.UpdateEvent;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class UpdateEventDispatcher
{
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event)
    {
        if (event.phase != Phase.END)
        {
            return;
        }

        ChudWare.EVENT_MANAGER.call(TickEvent.INSTANCE);
        ChudWare.EVENT_MANAGER.call(UpdateEvent.INSTANCE);
    }
}
