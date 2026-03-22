package com.lethalfart.ChudWare.eventbus.impl.misc;

import com.lethalfart.ChudWare.eventbus.impl.Event;

public class PreMotionAlwaysEvent extends Event
{
    public static final PreMotionAlwaysEvent INSTANCE = new PreMotionAlwaysEvent();
    private PreMotionAlwaysEvent() {}
}