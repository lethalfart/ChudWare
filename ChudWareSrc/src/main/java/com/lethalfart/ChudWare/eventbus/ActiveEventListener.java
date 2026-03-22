package com.lethalfart.ChudWare.eventbus;

import com.lethalfart.ChudWare.eventbus.impl.Event;

public interface ActiveEventListener
{
    boolean shouldHandleEvent(Class<? extends Event> eventClass);
}
