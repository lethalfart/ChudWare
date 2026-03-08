package com.lethalfart.ChudWare.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ChamsHandler
{
    private final ChamsModule chamsModule;
    private ResourceLocation gridTexture;

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
            renderGlow(model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, visibleBaseColor, hiddenBaseColor, mc, partial);
        }
        finally
        {
            GL11.glPopAttrib();
            GlStateManager.popMatrix();
        }
    }

    private void renderGlow(
            ModelBase model,
            EntityPlayer entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float headYaw,
            float pitch,
            int visibleBaseColor,
            int hiddenBaseColor,
            Minecraft mc,
            float partial
    )
    {
        float animationSpeed = 0.18F + (chamsModule.getAnimationSpeed() / 100.0F) * 0.85F;
        float glowStrength = chamsModule.getGlowStrength() / 100.0F;
        float outlineStrength = chamsModule.getOutlineWidth() / 100.0F;
        float time = ((System.currentTimeMillis() % 100000L) / 1000.0F) * animationSpeed;
        float visiblePulse = 0.82F + (0.18F * wave((time * 1.35F) + (entity.getEntityId() * 0.19F)));
        float hiddenPulse = 0.74F + (0.14F * wave((time * 1.18F) + (entity.getEntityId() * 0.16F)));
        int visibleColor = visibleBaseColor;
        int hiddenColor = hiddenBaseColor;
        int visibleRimColor = deriveGridColor(visibleBaseColor, glowStrength, visiblePulse);
        int hiddenRimColor = deriveGridColor(hiddenBaseColor, glowStrength * 0.85F, hiddenPulse);
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
        renderFlat(model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, occludedPassColor);
        renderSurfaceGrid(
                model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch,
                occludedPassRimColor,
                0.28F + (glowStrength * 0.12F),
                time,
                1.18F
        );

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        renderFlat(model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, visibleColor);
        renderSurfaceGrid(
                model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch,
                visibleRimColor,
                0.22F + (glowStrength * 0.16F),
                time,
                1.34F + (outlineStrength * 0.10F)
        );
        renderGlowShell(
                model,
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks,
                headYaw,
                pitch,
                brightenRgb(visibleRimColor, 22),
                1.012F + (glowStrength * 0.020F) + (visiblePulse * 0.010F),
                0.16F + (glowStrength * 0.20F) + ((visiblePulse - 0.8F) * 0.10F)
        );

        if (!anyVisible)
        {
            renderSurfaceGrid(
                    model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch,
                    occludedPassRimColor,
                    0.22F + (glowStrength * 0.08F),
                    time,
                    1.26F
            );
            renderGlowShell(
                    model,
                    entity,
                    limbSwing,
                    limbSwingAmount,
                    ageInTicks,
                    headYaw,
                    pitch,
                    brightenRgb(occludedPassRimColor, 14),
                    1.008F + (glowStrength * 0.014F),
                    0.08F + (glowStrength * 0.10F)
            );
        }

        GL11.glDepthFunc(GL11.GL_LEQUAL);
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

    private void renderSurfaceGrid(
            ModelBase model,
            EntityPlayer entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float headYaw,
            float pitch,
            int color,
            float alphaScale,
            float time,
            float uvScale
    )
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null)
        {
            return;
        }

        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-0.35F, -1000000.0F);
        mc.getTextureManager().bindTexture(getGridTexture(mc));

        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        float driftU = (time * 0.0045F) + (entity.getEntityId() * 0.0012F);
        float driftV = (time * -0.0030F) + (entity.getEntityId() * 0.0009F);
        GL11.glTranslatef(driftU, driftV, 0.0F);
        GL11.glScalef(uvScale, uvScale, 1.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        applyColor(withScaledAlpha(color, alphaScale));
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F);

        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glTranslatef(0.0125F, 0.0125F, 0.0F);
        GL11.glScalef(1.08F, 1.08F, 1.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        int innerColor = withScaledAlpha(brightenRgb(color, 14), 0.18F * alphaScale);
        applyColor(innerColor);
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F);

        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderGlowShell(
            ModelBase model,
            EntityPlayer entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float headYaw,
            float pitch,
            int color,
            float scale,
            float alphaScale
    )
    {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableTexture2D();
        GlStateManager.scale(scale, scale, scale);
        applyColor(withScaledAlpha(color, clamp01(alphaScale)));
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.popMatrix();
    }

    private ResourceLocation getGridTexture(Minecraft mc)
    {
        if (gridTexture == null)
        {
            DynamicTexture texture = new DynamicTexture(128, 128);
            int[] pixels = texture.getTextureData();
            for (int y = 0; y < 128; y++)
            {
                for (int x = 0; x < 128; x++)
                {
                    int index = (y * 128) + x;
                    float dx = Math.min(x % 12, 12 - (x % 12));
                    float dy = Math.min(y % 12, 12 - (y % 12));
                    float lineDistance = Math.min(dx, dy);
                    float alpha = 0.0F;
                    if (lineDistance < 0.65F)
                    {
                        alpha = 0.46F;
                    }
                    else if (lineDistance < 1.6F)
                    {
                        alpha = 0.14F;
                    }
                    pixels[index] = (clamp8(Math.round(alpha * 255.0F)) << 24) | 0xFFFFFF;
                }
            }
            texture.updateDynamicTexture();
            gridTexture = mc.getTextureManager().getDynamicTextureLocation("chams_grid", texture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }
        return gridTexture;
    }

    private int deriveGridColor(int argb, float glowStrength, float pulse)
    {
        int boosted = brightenRgb(argb, 14 + Math.round(glowStrength * 22.0F));
        int cooled = blendArgb(boosted, 0xFFF4FBFF, 0.05F + (pulse * 0.03F));
        return withScaledAlpha(cooled, 0.52F + (pulse * 0.08F));
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
