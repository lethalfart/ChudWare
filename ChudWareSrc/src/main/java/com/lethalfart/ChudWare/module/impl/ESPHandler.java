package com.lethalfart.ChudWare.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;

public class ESPHandler
{
    private final ESPModule espModule;

    private static Field timerField;
    private static Field renderPartialTicksField;
    private static boolean fieldsResolved = false;

    public ESPHandler(ESPModule espModule)
    {
        this.espModule = espModule;
        resolveFields();
    }

    private static void resolveFields()
    {
        if (fieldsResolved) return;
        fieldsResolved = true;
        try
        {
            for (Field f : Minecraft.class.getDeclaredFields())
            {
                if (f.getType().getSimpleName().equals("Timer"))
                {
                    f.setAccessible(true);
                    timerField = f;
                    break;
                }
            }

            if (timerField == null)
            {
                System.out.println("[ChudWare] ESP: could not find Timer field in Minecraft");
                return;
            }

            Object timer = timerField.get(Minecraft.getMinecraft());
            Class<?> timerClass = timer.getClass();

            for (String name : new String[]{ "renderPartialTicks", "field_74278_d", "elapsedPartialTicks" })
            {
                try
                {
                    Field f = timerClass.getDeclaredField(name);
                    f.setAccessible(true);
                    renderPartialTicksField = f;
                    break;
                }
                catch (NoSuchFieldException ignored) {}
            }

            if (renderPartialTicksField == null)
            {
                System.out.println("[ChudWare] ESP: dumping Timer fields for debug:");
                for (Field f : timerClass.getDeclaredFields())
                    System.out.println("[ChudWare]   " + f.getType().getSimpleName() + " " + f.getName());
            }
        }
        catch (Throwable e)
        {
            System.out.println("[ChudWare] ESP: resolveFields failed: " + e.getMessage());
        }
    }

    private float getPartialTicks()
    {
        try
        {
            if (timerField != null && renderPartialTicksField != null)
            {
                Object timer = timerField.get(Minecraft.getMinecraft());
                return renderPartialTicksField.getFloat(timer);
            }
        }
        catch (Throwable ignored) {}
        return 1.0F;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        if (!espModule.isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (mc.theWorld.playerEntities.isEmpty()) return;

        float pt = getPartialTicks();

        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        double maxDistSq = espModule.getMaxDistance();
        maxDistSq = maxDistSq * maxDistSq;

        GlStateManager.pushMatrix();
        try
        {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager.color(
                    espModule.getRed() / 255.0F,
                    espModule.getGreen() / 255.0F,
                    espModule.getBlue() / 255.0F,
                    espModule.getAlpha() / 255.0F
            );
            GL11.glLineWidth((float) espModule.getLineWidth());

            for (Object obj : mc.theWorld.playerEntities)
            {
                if (!(obj instanceof EntityPlayer)) continue;

                Entity entity = (Entity) obj;
                if (entity == mc.thePlayer || entity.isDead || !entity.isEntityAlive()) continue;
                if (!espModule.isShowInvisible() && entity.isInvisible()) continue;

                double dx = entity.posX - mc.thePlayer.posX;
                double dy = entity.posY - mc.thePlayer.posY;
                double dz = entity.posZ - mc.thePlayer.posZ;
                if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;



                double renderX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * pt - viewerX;
                double renderY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * pt - viewerY;
                double renderZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * pt - viewerZ;

                double halfW = entity.width / 2.0D + 0.05D;
                double height = entity.height + 0.1D;

                AxisAlignedBB bb = new AxisAlignedBB(
                        renderX - halfW, renderY,          renderZ - halfW,
                        renderX + halfW, renderY + height, renderZ + halfW
                );

                RenderGlobal.drawSelectionBoundingBox(bb);
            }
        }
        finally
        {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}
