package com.lethalfart.ChudWare.eventbus.impl.packet;

import com.lethalfart.ChudWare.eventbus.impl.Event;

public class PacketSendEvent extends Event
{
    private final Object packet;
    private boolean cancelled;

    public PacketSendEvent(Object packet)
    {
        this.packet = packet;
    }

    public Object getPacket()
    {
        return packet;
    }

    public boolean isCancelled()
    {
        return cancelled;
    }

    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }
}
