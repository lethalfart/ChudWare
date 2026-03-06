package com.lethalfart.ChudWare.eventbus.impl.player;

import com.lethalfart.ChudWare.eventbus.impl.Event;

public class UpdateEvent extends Event
{
    public static final UpdateEvent INSTANCE = new UpdateEvent();

    private UpdateEvent()
    {
    }
}
