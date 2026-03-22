package com.lethalfart.ChudWare.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class ESPHandler
{
    private static final int FRIEND_RGB = 0x55FFFF;

    private final ESPModule espModule;

    public ESPHandler(ESPModule espModule)
    {
        this.espModule = espModule;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        if (!espModule.isEnabled())
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null)
        {
            return;
        }

        List<Entity> entities = mc.theWorld.loadedEntityList;
        if (entities == null || entities.isEmpty())
        {
            return;
        }

        float partialTicks = event.partialTicks;
        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();
        try
        {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glLineWidth(2.0F);
            GlStateManager.depthMask(false);

            for (Entity entity : entities)
            {
                if (!(entity instanceof EntityLivingBase))
                {
                    continue;
                }

                EntityLivingBase living = (EntityLivingBase) entity;
                if (!espModule.isValid(living))
                {
                    continue;
                }

                int argb = getOutlineColor(living);
                applyColor(argb);

                double interpX = entity.lastTickPosX + ((entity.posX - entity.lastTickPosX) * partialTicks);
                double interpY = entity.lastTickPosY + ((entity.posY - entity.lastTickPosY) * partialTicks);
                double interpZ = entity.lastTickPosZ + ((entity.posZ - entity.lastTickPosZ) * partialTicks);
                AxisAlignedBB entityBox = entity.getEntityBoundingBox();
                AxisAlignedBB bb = new AxisAlignedBB(
                        entityBox.minX - entity.posX + interpX - viewerX,
                        entityBox.minY - entity.posY + interpY - viewerY,
                        entityBox.minZ - entity.posZ + interpZ - viewerZ,
                        entityBox.maxX - entity.posX + interpX - viewerX,
                        entityBox.maxY - entity.posY + interpY - viewerY + 0.05D,
                        entityBox.maxZ - entity.posZ + interpZ - viewerZ
                ).expand(0.05D, 0.05D, 0.05D);

                drawOutlinedBox(bb);
            }
        }
        finally
        {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.depthMask(true);
            GL11.glPopAttrib();
            GlStateManager.popMatrix();
        }
    }

    private int getOutlineColor(EntityLivingBase entity)
    {
        if (isFriend(entity))
        {
            return ((espModule.getAlpha() & 0xFF) << 24) | FRIEND_RGB;
        }

        return toArgb(espModule.getRed(), espModule.getGreen(), espModule.getBlue(), espModule.getAlpha());
    }

    private boolean isFriend(EntityLivingBase entity)
    {
        return entity instanceof EntityPlayer && AimAssistModule.isFriend(entity.getName());
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

    private void drawOutlinedBox(AxisAlignedBB bb)
    {
        double minX = bb.minX;
        double minY = bb.minY;
        double minZ = bb.minZ;
        double maxX = bb.maxX;
        double maxY = bb.maxY;
        double maxZ = bb.maxZ;

        GL11.glBegin(GL11.GL_LINES);

        vertex(minX, minY, minZ); vertex(maxX, minY, minZ);
        vertex(maxX, minY, minZ); vertex(maxX, minY, maxZ);
        vertex(maxX, minY, maxZ); vertex(minX, minY, maxZ);
        vertex(minX, minY, maxZ); vertex(minX, minY, minZ);

        vertex(minX, maxY, minZ); vertex(maxX, maxY, minZ);
        vertex(maxX, maxY, minZ); vertex(maxX, maxY, maxZ);
        vertex(maxX, maxY, maxZ); vertex(minX, maxY, maxZ);
        vertex(minX, maxY, maxZ); vertex(minX, maxY, minZ);

        vertex(minX, minY, minZ); vertex(minX, maxY, minZ);
        vertex(maxX, minY, minZ); vertex(maxX, maxY, minZ);
        vertex(maxX, minY, maxZ); vertex(maxX, maxY, maxZ);
        vertex(minX, minY, maxZ); vertex(minX, maxY, maxZ);

        GL11.glEnd();
    }

    private void vertex(double x, double y, double z)
    {
        GL11.glVertex3d(x, y, z);
    }

    private int toArgb(int r, int g, int b, int a)
    {
        return ((a & 0xFF) << 24)
                | ((r & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | (b & 0xFF);
    }
}
