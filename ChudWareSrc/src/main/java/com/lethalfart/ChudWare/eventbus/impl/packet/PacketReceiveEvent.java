package com.lethalfart.ChudWare.eventbus.impl.packet;

import com.lethalfart.ChudWare.eventbus.impl.Event;

public class PacketReceiveEvent extends Event
{
    private Object packet;
    private boolean cancelled;

    public PacketReceiveEvent(Object packet)
    {
        this.packet = packet;
    }

    public Object getPacket()
    {
        return packet;
    }

    public void setPacket(Object packet)
    {
        this.packet = packet;
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