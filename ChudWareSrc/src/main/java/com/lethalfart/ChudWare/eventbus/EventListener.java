package com.lethalfart.ChudWare.eventbus;

import com.lethalfart.ChudWare.eventbus.impl.Event;

public interface EventListener
{
    void invoke(Event event) throws Exception;
}
