package com.lethalfart.ChudWare.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.UUID;

public class ChamsHandler
{
    private static final long TARGET_MEMORY_MS = 4000L;
    private static final int FRIEND_VISIBLE_RGB = 0x55FFFF;
    private static final int FRIEND_HIDDEN_RGB = 0x2F7DFF;

    private final ChamsModule chamsModule;
    private UUID trackedTargetId;
    private long trackedTargetUntil;

    public ChamsHandler(ChamsModule chamsModule)
    {
        this.chamsModule = chamsModule;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        if (System.currentTimeMillis() > trackedTargetUntil)
        {
            trackedTargetId = null;
            trackedTargetUntil = 0L;
        }

        if (chamsModule.isEnabled())
        {
            chamsModule.tickPulse();
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || event.entityPlayer != mc.thePlayer)
        {
            return;
        }

        if (event.target instanceof EntityLivingBase)
        {
            trackedTargetId = event.target.getUniqueID();
            trackedTargetUntil = System.currentTimeMillis() + TARGET_MEMORY_MS;
        }
    }

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre event)
    {
        EntityLivingBase entity = event.entity;
        if (!isValid(entity))
        {
            return;
        }

        if (chamsModule.isColored())
        {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

            int color = Minecraft.getMinecraft().thePlayer.canEntityBeSeen(entity)
                    ? getVisibleColor(entity)
                    : getHiddenColor(entity);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);

            if (chamsModule.isMaterial())
            {
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glShadeModel(GL11.GL_SMOOTH);
            }
            else
            {
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glShadeModel(GL11.GL_FLAT);
            }

            if (!Minecraft.getMinecraft().thePlayer.canEntityBeSeen(entity))
            {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GlStateManager.depthMask(false);
            }

            applyColor(color);
            return;
        }

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1.0F, -1100000.0F);
    }

    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post event)
    {
        EntityLivingBase entity = event.entity;
        if (!isValid(entity))
        {
            return;
        }

        if (chamsModule.isColored())
        {
            GlStateManager.depthMask(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopAttrib();
            return;
        }

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1.0F, 1100000.0F);
    }

    private boolean isValid(EntityLivingBase entity)
    {
        Minecraft mc = Minecraft.getMinecraft();
        return chamsModule.isEnabled()
                && mc.thePlayer != null
                && mc.theWorld != null
                && entity != null
                && entity.isEntityAlive()
                && !entity.isInvisible()
                && entity != mc.thePlayer
                && isValidType(entity)
                && matchesTargetFilter(entity, mc);
    }

    private boolean isValidType(EntityLivingBase entity)
    {
        return (chamsModule.isTargetPlayers() && entity instanceof EntityPlayer)
                || (chamsModule.isTargetMobs() && (entity instanceof EntityMob || entity instanceof EntitySlime))
                || (chamsModule.isTargetPassives() && (entity instanceof EntityVillager || entity instanceof EntityGolem))
                || (chamsModule.isTargetAnimals() && entity instanceof EntityAnimal);
    }

    private boolean matchesTargetFilter(EntityLivingBase entity, Minecraft mc)
    {
        if (!chamsModule.isOnlyTargets())
        {
            return true;
        }

        Entity pointed = mc.pointedEntity;
        if (pointed == entity)
        {
            return true;
        }

        return trackedTargetId != null
                && System.currentTimeMillis() <= trackedTargetUntil
                && trackedTargetId.equals(entity.getUniqueID());
    }

    private int getVisibleColor(EntityLivingBase entity)
    {
        if (isFriend(entity))
        {
            return withAlpha(FRIEND_VISIBLE_RGB, chamsModule.getRenderedVisibleAlpha());
        }

        return chamsModule.isRainbow()
                ? rainbowArgb(0L, chamsModule.getRenderedVisibleAlpha())
                : toArgb(
                chamsModule.getVisibleRed(),
                chamsModule.getVisibleGreen(),
                chamsModule.getVisibleBlue(),
                chamsModule.getRenderedVisibleAlpha()
        );
    }

    private int getHiddenColor(EntityLivingBase entity)
    {
        if (isFriend(entity))
        {
            return withAlpha(FRIEND_HIDDEN_RGB, chamsModule.getHiddenAlpha());
        }

        return chamsModule.isRainbow()
                ? rainbowArgb(450L, chamsModule.getHiddenAlpha())
                : toArgb(
                chamsModule.getHiddenRed(),
                chamsModule.getHiddenGreen(),
                chamsModule.getHiddenBlue(),
                chamsModule.getHiddenAlpha()
        );
    }

    private boolean isFriend(EntityLivingBase entity)
    {
        return entity instanceof EntityPlayer && AimAssistModule.isFriend(entity.getName());
    }

    private int withAlpha(int rgb, int alpha)
    {
        return ((clamp8(alpha) & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private int rainbowArgb(long offsetMs, int alpha)
    {
        float hue = ((System.currentTimeMillis() + offsetMs) % 3600L) / 3600.0F;
        int rgb = Color.HSBtoRGB(hue, 0.85F, 1.0F);
        return ((clamp8(alpha) & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private void applyColor(int argb)
    {
        GlStateManager.color(
                ((argb >>> 16) & 0xFF) / 255.0F,
                ((argb >>> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F,
                ((argb >>> 24) & 0xFF) / 255.0F
        );
    }

    private int toArgb(int r, int g, int b, int a)
    {
        return (clamp8(a) << 24) | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b);
    }

    private int clamp8(int value)
    {
        if (value < 0)
        {
            return 0;
        }
        if (value > 255)
        {
            return 255;
        }
        return value;
    }

}
