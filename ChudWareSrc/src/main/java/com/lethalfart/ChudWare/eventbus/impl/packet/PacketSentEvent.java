package com.lethalfart.ChudWare.eventbus.impl.packet;

import com.lethalfart.ChudWare.eventbus.impl.Event;

public class PacketSentEvent extends Event
{
    private final Object packet;

    public PacketSentEvent(Object packet)
    {
        this.packet = packet;
    }

    public Object getPacket()
    {
        return packet;
    }
}
