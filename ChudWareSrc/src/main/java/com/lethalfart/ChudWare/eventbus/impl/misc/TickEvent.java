package com.lethalfart.ChudWare.eventbus.impl.misc;

import com.lethalfart.ChudWare.eventbus.impl.Event;

public class TickEvent extends Event
{
    public static final TickEvent INSTANCE = new TickEvent();

    private TickEvent()
    {
    }
}
