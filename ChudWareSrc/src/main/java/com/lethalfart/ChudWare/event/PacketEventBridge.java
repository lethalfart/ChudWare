package com.lethalfart.ChudWare.event;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.impl.packet.PacketReceiveEvent;
import com.lethalfart.ChudWare.eventbus.impl.packet.PacketSendEvent;
import com.lethalfart.ChudWare.eventbus.impl.packet.PacketSentEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

public class PacketEventBridge
{
    private static final String HANDLER_NAME = "chudware_packet_hook";

    private static Field cachedChannelField = null;
    private static boolean channelFieldResolved = false;

    private Channel hookedChannel;
    private boolean hookAttempted;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getNetHandler() == null || mc.getNetHandler().getNetworkManager() == null)
        {
            unhook();
            hookAttempted = false;
            return;
        }

        NetworkManager networkManager = mc.getNetHandler().getNetworkManager();
        Channel channel = resolveChannel(networkManager);
        if (channel == null || !channel.isOpen())
        {
            unhook();
            return;
        }

        if (channel == hookedChannel && channel.pipeline().get(HANDLER_NAME) != null) return;

        if (!hookAttempted || hookedChannel != channel)
        {
            hookAttempted = true;
            inject(channel);
        }
    }

    private void inject(Channel channel)
    {
        try
        {
            if (channel.pipeline().get(HANDLER_NAME) != null)
            {
                hookedChannel = channel;
                return;
            }

            channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new ChannelDuplexHandler()
            {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
                {
                    PacketReceiveEvent event = new PacketReceiveEvent(msg);
                    ChudWare.EVENT_MANAGER.call(event);
                    if (!event.isCancelled())
                    {
                        super.channelRead(ctx, msg);
                    }
                }

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
                {
                    PacketSendEvent event = new PacketSendEvent(msg);
                    ChudWare.EVENT_MANAGER.call(event);
                    if (!event.isCancelled())
                    {
                        super.write(ctx, msg, promise);


                        ChudWare.EVENT_MANAGER.call(new PacketSentEvent(msg));
                    }
                }
            });

            hookedChannel = channel;
            System.out.println("[ChudWare] Packet hook injected successfully!");
        }
        catch (Throwable e)
        {
            System.out.println("[ChudWare] Failed to inject packet hook: " + e.getMessage());
        }
    }

    private void unhook()
    {
        try
        {
            if (hookedChannel != null && hookedChannel.pipeline().get(HANDLER_NAME) != null)
            {
                hookedChannel.pipeline().remove(HANDLER_NAME);
            }
        }
        catch (Throwable ignored) {}
        finally
        {
            hookedChannel = null;
        }
    }

    private Channel resolveChannel(NetworkManager networkManager)
    {
        if (!channelFieldResolved)
        {
            channelFieldResolved = true;
            for (Field f : NetworkManager.class.getDeclaredFields())
            {
                if (f.getType() == Channel.class)
                {
                    try
                    {
                        f.setAccessible(true);
                        cachedChannelField = f;
                        break;
                    }
                    catch (Exception ignored) {}
                }
            }
            if (cachedChannelField == null)
            {
                System.out.println("[ChudWare] Failed to resolve channel field!");
            }
        }

        if (cachedChannelField == null) return null;

        try
        {
            Object value = cachedChannelField.get(networkManager);
            if (value instanceof Channel) return (Channel) value;
        }
        catch (Exception ignored) {}

        return null;
    }
}
