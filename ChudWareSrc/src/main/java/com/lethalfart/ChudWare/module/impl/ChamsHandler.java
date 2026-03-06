package com.lethalfart.ChudWare.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ChamsHandler
{
    private final ChamsModule chamsModule;

    public ChamsHandler(ChamsModule chamsModule)
    {
        this.chamsModule = chamsModule;
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event)
    {
        if (!chamsModule.isEnabled())
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }

        EntityPlayer entity = event.entityPlayer;
        if (entity == mc.thePlayer || !entity.isEntityAlive())
        {
            return;
        }

        ModelBase model = event.renderer.getMainModel();
        float partial = event.partialRenderTick;
        float bodyYaw = interpolate(entity.prevRenderYawOffset, entity.renderYawOffset, partial);
        float headYaw = interpolate(entity.prevRotationYawHead, entity.rotationYawHead, partial) - bodyYaw;
        float pitch = entity.prevRotationPitch + ((entity.rotationPitch - entity.prevRotationPitch) * partial);
        float limbSwingAmount = entity.prevLimbSwingAmount + ((entity.limbSwingAmount - entity.prevLimbSwingAmount) * partial);
        float limbSwing = entity.limbSwing - (entity.limbSwingAmount * (1.0F - partial));
        float ageInTicks = entity.ticksExisted + partial;

        if (entity.isChild())
        {
            limbSwing *= 3.0F;
        }
        if (limbSwingAmount > 1.0F)
        {
            limbSwingAmount = 1.0F;
        }

        GlStateManager.pushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try
        {
            GlStateManager.translate(event.x, event.y, event.z);
            GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.enableRescaleNormal();
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            GlStateManager.translate(0.0F, -1.5078125F, 0.0F);

            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableAlpha();
            GlStateManager.disableCull();

            model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partial);
            model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F, entity);

            int visibleBaseColor = toArgb(
                    chamsModule.getVisibleRed(),
                    chamsModule.getVisibleGreen(),
                    chamsModule.getVisibleBlue(),
                    chamsModule.getVisibleAlpha()
            );
            int hiddenBaseColor = toArgb(
                    chamsModule.getHiddenRed(),
                    chamsModule.getHiddenGreen(),
                    chamsModule.getHiddenBlue(),
                    chamsModule.getHiddenAlpha()
            );
            float time = (System.currentTimeMillis() % 100000L) / 1000.0F;
            float visiblePulse = 0.60F + (0.40F * wave((time * 3.3F) + (entity.getEntityId() * 0.31F)));
            float hiddenPulse = 0.52F + (0.32F * wave((time * 2.8F) + (entity.getEntityId() * 0.24F)));
            int visibleColor = withScaledAlpha(visibleBaseColor, visiblePulse);
            int hiddenColor = withScaledAlpha(hiddenBaseColor, hiddenPulse);
            int visibleRimColor = brightenRgb(withScaledAlpha(visibleBaseColor, 245.0F / 255.0F), 88);
            int hiddenRimColor = brightenRgb(withScaledAlpha(hiddenBaseColor, 205.0F / 255.0F), 56);
            float hurtFlash = 0.0F;
            if (entity.hurtTime > 0)
            {
                int maxHurt = Math.max(1, entity.maxHurtTime);
                float hurtTicks = Math.max(0.0F, entity.hurtTime - partial);
                hurtFlash = clamp01(hurtTicks / maxHurt);
            }
            if (hurtFlash > 0.0F)
            {
                int hurtDarkRed = toArgb(124, 16, 16, 255);
                visibleColor = blendArgb(visibleColor, hurtDarkRed, hurtFlash);
                hiddenColor = blendArgb(hiddenColor, hurtDarkRed, hurtFlash);
                visibleRimColor = blendArgb(visibleRimColor, brightenRgb(hurtDarkRed, 30), hurtFlash);
                hiddenRimColor = blendArgb(hiddenRimColor, brightenRgb(hurtDarkRed, 30), hurtFlash);
            }
            boolean anyVisible = mc.thePlayer.canEntityBeSeen(entity);
            int occludedPassColor = anyVisible ? visibleColor : hiddenColor;
            int occludedPassRimColor = anyVisible ? visibleRimColor : hiddenRimColor;

            
            GlStateManager.enableDepth();
            GlStateManager.depthMask(false);
            GL11.glDepthFunc(GL11.GL_GREATER);
            renderFlat(
                    model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch,
                    occludedPassColor
            );

            
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            renderFlat(
                    model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch,
                    visibleColor
            );

            
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            GL11.glLineWidth(1.15F + (0.75F * visiblePulse));
            int outlineAlpha = Math.min(235, ((visibleColor >>> 24) & 0xFF) + 38);
            int outlineColor = (outlineAlpha << 24) | (visibleColor & 0x00FFFFFF);
            applyColor(outlineColor);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
        finally
        {
            GL11.glPopAttrib();
            GlStateManager.popMatrix();
        }
    }

    private float interpolate(float prev, float current, float partial)
    {
        return prev + ((current - prev) * partial);
    }

    private void renderFlat(
            ModelBase model,
            EntityPlayer entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float headYaw,
            float pitch,
            int color
    )
    {
        applyColor(color);
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F);
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

    private int withScaledAlpha(int argb, float factor)
    {
        int alpha = (argb >>> 24) & 0xFF;
        int scaled = clamp8(Math.round(alpha * factor));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    private int brightenRgb(int argb, int amount)
    {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + amount);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + amount);
        int b = Math.min(255, (argb & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float wave(float input)
    {
        return 0.5F + (0.5F * (float) Math.sin(input));
    }

    private int blendArgb(int from, int to, float t)
    {
        float clampedT = clamp01(t);
        int fromA = (from >>> 24) & 0xFF;
        int fromR = (from >>> 16) & 0xFF;
        int fromG = (from >>> 8) & 0xFF;
        int fromB = from & 0xFF;

        int toA = (to >>> 24) & 0xFF;
        int toR = (to >>> 16) & 0xFF;
        int toG = (to >>> 8) & 0xFF;
        int toB = to & 0xFF;

        int a = clamp8(Math.round(fromA + ((toA - fromA) * clampedT)));
        int r = clamp8(Math.round(fromR + ((toR - fromR) * clampedT)));
        int g = clamp8(Math.round(fromG + ((toG - fromG) * clampedT)));
        int b = clamp8(Math.round(fromB + ((toB - fromB) * clampedT)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float clamp01(float v)
    {
        if (v < 0.0F)
        {
            return 0.0F;
        }
        if (v > 1.0F)
        {
            return 1.0F;
        }
        return v;
    }

    private int clamp8(int v)
    {
        if (v < 0)
        {
            return 0;
        }
        if (v > 255)
        {
            return 255;
        }
        return v;
    }
}
