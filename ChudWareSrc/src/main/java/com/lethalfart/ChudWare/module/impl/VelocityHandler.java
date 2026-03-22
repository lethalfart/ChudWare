package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import com.lethalfart.ChudWare.eventbus.ActiveEventListener;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.Event;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;
import com.lethalfart.ChudWare.eventbus.impl.packet.PacketReceiveEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Random;

public class VelocityHandler implements ActiveEventListener
{
    private final VelocityModule velocityModule;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    private int hitCount = 0;
    private volatile boolean packetHandlingEnabled;
    private volatile int playerEntityId = -1;
    private volatile boolean skipForEnvironment;
    private volatile int chance;
    private volatile double horizontalPercent;
    private volatile double verticalPercent;
    private volatile boolean bufferMode;
    private volatile int bufferTicks;

    private Field velocityMotionX;
    private Field velocityMotionY;
    private Field velocityMotionZ;

    private Field explosionMotionX;
    private Field explosionMotionY;
    private Field explosionMotionZ;

    public VelocityHandler(VelocityModule velocityModule)
    {
        this.velocityModule = velocityModule;
        initVelocityFields();
        initExplosionFields();
        prewarm();
        ChudWare.EVENT_MANAGER.register(this);
    }

    private void initVelocityFields()
    {
        try { velocityMotionX = resolveField(S12PacketEntityVelocity.class, "motionX", "field_149415_b"); } catch (NoSuchFieldException e) { System.out.println("[ChudWare] Could not cache velocityMotionX: " + e.getMessage()); }
        try { velocityMotionY = resolveField(S12PacketEntityVelocity.class, "motionY", "field_149416_c"); } catch (NoSuchFieldException e) { System.out.println("[ChudWare] Could not cache velocityMotionY: " + e.getMessage()); }
        try { velocityMotionZ = resolveField(S12PacketEntityVelocity.class, "motionZ", "field_149414_d"); } catch (NoSuchFieldException e) { System.out.println("[ChudWare] Could not cache velocityMotionZ: " + e.getMessage()); }
    }

    private void initExplosionFields()
    {
        try { explosionMotionX = resolveField(S27PacketExplosion.class, "motionX", "field_149152_f"); } catch (NoSuchFieldException e) { System.out.println("[ChudWare] Could not cache explosionMotionX: " + e.getMessage()); }
        try { explosionMotionY = resolveField(S27PacketExplosion.class, "motionY", "field_149153_g"); } catch (NoSuchFieldException e) { System.out.println("[ChudWare] Could not cache explosionMotionY: " + e.getMessage()); }
        try { explosionMotionZ = resolveField(S27PacketExplosion.class, "motionZ", "field_149159_h"); } catch (NoSuchFieldException e) { System.out.println("[ChudWare] Could not cache explosionMotionZ: " + e.getMessage()); }
    }

    private void prewarm()
    {
        try
        {
            S12PacketEntityVelocity dummy = new S12PacketEntityVelocity(0, 0, 0, 0);
            handleVelocityPacket(new PacketReceiveEvent(dummy), dummy, 0, 100.0D, 100.0D, false, 1);
        }
        catch (Throwable ignored) {}

        try
        {
            S27PacketExplosion dummyExp = new S27PacketExplosion(0.0, 0.0, 0.0, 0.0f, Collections.emptyList(), new Vec3(0, 0, 0));
            handleExplosionPacket(new PacketReceiveEvent(dummyExp), dummyExp, 100.0D, 100.0D);
        }
        catch (Throwable ignored) {}
    }

    @EventTarget
    public void onTick(TickEvent event)
    {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null)
        {
            packetHandlingEnabled = false;
            playerEntityId = -1;
            skipForEnvironment = false;
            hitCount = 0;
            return;
        }

        packetHandlingEnabled = true;
        playerEntityId = mc.thePlayer.getEntityId();
        chance = velocityModule.getChance();
        horizontalPercent = velocityModule.getHorizontalPercent();
        verticalPercent = velocityModule.getVerticalPercent();
        bufferMode = velocityModule.isBufferMode();
        bufferTicks = Math.max(1, velocityModule.getBufferTicks());
        skipForEnvironment =
                (velocityModule.isNoWater() && mc.thePlayer.isInWater())
                        || (velocityModule.isNoLava() && mc.thePlayer.isInLava())
                        || (velocityModule.isNoLadder() && mc.thePlayer.isOnLadder());
    }

    private boolean isEnabled()
    {
        return velocityModule != null && velocityModule.isEnabled();
    }

    @EventTarget
    public void onPacketReceive(PacketReceiveEvent event)
    {
        if (!packetHandlingEnabled) return;
        if (skipForEnvironment)
        {
            resetBufferState();
            return;
        }

        double roll = Math.random() * 100;
        if (roll > chance) return;

        if (event.getPacket() instanceof S12PacketEntityVelocity)
        {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            handleVelocityPacket(event, packet, playerEntityId, horizontalPercent, verticalPercent, bufferMode, bufferTicks);
        }
        else if (event.getPacket() instanceof S27PacketExplosion)
        {
            handleExplosionPacket(event, (S27PacketExplosion) event.getPacket(), horizontalPercent, verticalPercent);
        }
    }


    private void handleVelocityPacket(PacketReceiveEvent event, S12PacketEntityVelocity packet, int currentPlayerEntityId, double h, double v, boolean currentBufferMode, int currentBufferTicks)
    {
        if (currentPlayerEntityId == -1) return;
        if (packet.getEntityID() != currentPlayerEntityId) return;

        if (h <= 0.0D && v <= 0.0D)
        {
            event.setCancelled(true);
            resetBufferState();
            return;
        }

        if (h >= 100.0D && v >= 100.0D) return;

        if (currentBufferMode)
        {
            hitCount++;
            if (hitCount < currentBufferTicks) return;
            hitCount = 0;
        }

        if (velocityMotionX == null || velocityMotionY == null || velocityMotionZ == null) return;

        try
        {
            int motionX = packet.getMotionX();
            int motionY = packet.getMotionY();
            int motionZ = packet.getMotionZ();

            double noisedH = h + (random.nextGaussian() * 3.5);
            double noisedV = v + (random.nextGaussian() * 3.5);
            noisedH = Math.max(0.0, Math.min(99.9, noisedH));
            noisedV = Math.max(0.0, Math.min(99.9, noisedV));

            int newMotionX = clampVelocityInt((motionX / 8000.0D) * (noisedH / 100.0D));
            int newMotionY = clampVelocityInt((motionY / 8000.0D) * (noisedV / 100.0D));
            int newMotionZ = clampVelocityInt((motionZ / 8000.0D) * (noisedH / 100.0D));

            if (newMotionX != motionX) velocityMotionX.setInt(packet, newMotionX);
            if (newMotionY != motionY) velocityMotionY.setInt(packet, newMotionY);
            if (newMotionZ != motionZ) velocityMotionZ.setInt(packet, newMotionZ);
        }
        catch (Exception e)
        {
            System.out.println("[ChudWare] handleVelocityPacket failed: " + e.getMessage());
        }
    }

    private void handleExplosionPacket(PacketReceiveEvent event, S27PacketExplosion packet, double h, double v)
    {
        if (h <= 0.0D && v <= 0.0D)
        {
            event.setCancelled(true);
            return;
        }

        if (h >= 100.0D && v >= 100.0D) return;

        if (explosionMotionX == null || explosionMotionY == null || explosionMotionZ == null) return;

        try
        {
            Object rawX = explosionMotionX.get(packet);
            Object rawY = explosionMotionY.get(packet);
            Object rawZ = explosionMotionZ.get(packet);

            float motionX = rawX instanceof Number ? ((Number) rawX).floatValue() : 0f;
            float motionY = rawY instanceof Number ? ((Number) rawY).floatValue() : 0f;
            float motionZ = rawZ instanceof Number ? ((Number) rawZ).floatValue() : 0f;

            double noisedH = h + (random.nextGaussian() * 3.5);
            double noisedV = v + (random.nextGaussian() * 3.5);
            noisedH = Math.max(0.0, Math.min(99.9, noisedH));
            noisedV = Math.max(0.0, Math.min(99.9, noisedV));

            explosionMotionX.setFloat(packet, (float)(motionX * (noisedH / 100.0D)));
            explosionMotionY.setFloat(packet, (float)(motionY * (noisedV / 100.0D)));
            explosionMotionZ.setFloat(packet, (float)(motionZ * (noisedH / 100.0D)));
        }
        catch (Exception e)
        {
            System.out.println("[ChudWare] handleExplosionPacket failed: " + e.getMessage());
        }
    }

    @Override
    public boolean shouldHandleEvent(Class<? extends Event> eventClass)
    {
        if (!isEnabled())
        {
            return false;
        }
        return eventClass == TickEvent.class || eventClass == PacketReceiveEvent.class;
    }

    private void resetBufferState()
    {
        hitCount = 0;
    }

    private int clampVelocityInt(double motionBlocks)
    {
        int scaled = (int) Math.round(motionBlocks * 8000.0D);
        return MathHelper.clamp_int(scaled, -32768, 32767);
    }

    private Field resolveField(Class<?> clazz, String deobfName, String obfName) throws NoSuchFieldException
    {
        try
        {
            Field field = clazz.getDeclaredField(deobfName);
            field.setAccessible(true);
            return field;
        }
        catch (NoSuchFieldException ignored)
        {
            Field field = clazz.getDeclaredField(obfName);
            field.setAccessible(true);
            return field;
        }
    }
}
