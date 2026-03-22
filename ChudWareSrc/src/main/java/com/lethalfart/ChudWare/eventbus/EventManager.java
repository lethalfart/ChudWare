package com.lethalfart.ChudWare.eventbus;

import com.lethalfart.ChudWare.eventbus.annotations.EventPriority;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.Event;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager
{
    private static class ListenerEntry
    {
        final Object owner;
        final EventListener listener;
        final int priority;

        ListenerEntry(Object owner, EventListener listener, int priority)
        {
            this.owner = owner;
            this.listener = listener;
            this.priority = priority;
        }

        boolean isActiveFor(Class<? extends Event> eventClass)
        {
            if (owner instanceof ActiveEventListener)
            {
                return ((ActiveEventListener) owner).shouldHandleEvent(eventClass);
            }
            return true;
        }
    }

    private static final Comparator<ListenerEntry> PRIORITY_COMPARATOR =
            Comparator.comparingInt(e -> e.priority);


    private final Map<Class<? extends Event>, List<ListenerEntry>> listenerMap = new ConcurrentHashMap<>();


    private final Map<Object, List<ListenerEntry>> objectListenerMap = new ConcurrentHashMap<>();

    public void register(Object... objects)
    {
        for (Object obj : objects)
        {
            register(obj);
        }
    }

    public void register(Object obj)
    {
        List<ListenerEntry> entries = new CopyOnWriteArrayList<>();

        for (Method method : obj.getClass().getDeclaredMethods())
        {
            if (method.getAnnotation(EventTarget.class) == null) continue;
            if (method.getParameterTypes().length != 1) continue;

            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) paramType;

            method.setAccessible(true);

            int priority = getPriority(method);


            EventListener listener = event -> method.invoke(obj, event);

            ListenerEntry entry = new ListenerEntry(obj, listener, priority);
            entries.add(entry);

            List<ListenerEntry> eventListeners = listenerMap.computeIfAbsent(
                    eventClass, k -> new CopyOnWriteArrayList<>());
            eventListeners.add(entry);
            ((CopyOnWriteArrayList<ListenerEntry>) eventListeners).sort(PRIORITY_COMPARATOR);
        }

        if (!entries.isEmpty())
        {
            objectListenerMap.put(obj, entries);
        }
    }

    public void unregister(Object obj)
    {
        List<ListenerEntry> entries = objectListenerMap.remove(obj);
        if (entries == null) return;

        for (Map.Entry<Class<? extends Event>, List<ListenerEntry>> mapEntry : listenerMap.entrySet())
        {
            mapEntry.getValue().removeAll(entries);
        }
    }

    public Event call(Event event)
    {
        List<ListenerEntry> entries = listenerMap.get(event.getClass());
        if (entries == null) return event;

        for (ListenerEntry entry : entries)
        {
            if (!entry.isActiveFor(event.getClass()))
            {
                continue;
            }
            try
            {
                entry.listener.invoke(event);
            }
            catch (Exception e)
            {
                System.out.println("[ChudWare] EventManager error: " + e.getCause());
                if (e.getCause() != null) e.getCause().printStackTrace();
            }
        }

        return event;
    }

    public boolean hasListeners(Class<? extends Event> eventClass)
    {
        List<ListenerEntry> entries = listenerMap.get(eventClass);
        if (entries == null || entries.isEmpty())
        {
            return false;
        }
        for (ListenerEntry entry : entries)
        {
            if (entry.isActiveFor(eventClass))
            {
                return true;
            }
        }
        return false;
    }

    private static int getPriority(Method method)
    {
        EventPriority priority = method.getAnnotation(EventPriority.class);
        return (priority != null) ? priority.value() : 10;
    }
}
