package com.lethalfart.ChudWare.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import com.lethalfart.ChudWare.module.impl.shader.ChamsShader;
import com.lethalfart.ChudWare.ui.modern.GuiTheme;
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
        float anim = clamp01(chamsModule.getAnimationSpeed() / 100.0F);
        float glowStrength = clamp01(chamsModule.getGlowStrength() / 100.0F);
        float outlineStrength = clamp01(chamsModule.getOutlineWidth() / 100.0F);
        float rimPower = clamp(2.6F - (outlineStrength * 1.4F), 1.1F, 2.8F);
        float time = ((System.currentTimeMillis() % 100000L) / 1000.0F);
        float pulseAmp = 0.04F + (0.06F * anim);
        float pulse = 1.0F + (float) Math.sin(time * (1.4F + (2.4F * anim))) * pulseAmp;

        int visibleColor = visibleBaseColor;
        int hiddenColor = hiddenBaseColor;
        int visibleGlow = buildGlowColor(visibleBaseColor, GuiTheme.ACCENT, glowStrength);
        int hiddenGlow = buildGlowColor(hiddenBaseColor, GuiTheme.ACCENT_2, glowStrength * 0.9F);
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
            visibleGlow = blendArgb(visibleGlow, brightenRgb(hurtDarkRed, 30), hurtFlash);
            hiddenGlow = blendArgb(hiddenGlow, brightenRgb(hurtDarkRed, 30), hurtFlash);
        }
        boolean anyVisible = mc.thePlayer.canEntityBeSeen(entity);
        int occludedPassColor = anyVisible ? visibleColor : hiddenColor;
        int occludedGlow = anyVisible ? visibleGlow : hiddenGlow;

        if (ChamsShader.isAvailable())
        {
            float glow = clamp01(glowStrength * pulse);

            GlStateManager.enableDepth();
            GlStateManager.depthMask(false);
            GL11.glDepthFunc(GL11.GL_GREATER);
            ChamsShader.use(occludedPassColor, occludedGlow, glow * 0.85F, rimPower + 0.25F);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F);

            GlStateManager.depthMask(true);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            ChamsShader.use(visibleColor, visibleGlow, glow, rimPower);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F);

            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            ChamsShader.use(withScaledAlpha(visibleColor, 0.05F), visibleGlow, glow * 0.75F, rimPower + 0.35F);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, 0.0625F);
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ChamsShader.stop();
        }
        else
        {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            renderFlat(model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, pitch, visibleColor);
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

    private int buildGlowColor(int baseColor, int accentColor, float strength)
    {
        int base = (baseColor & 0x00FFFFFF) | 0xFF000000;
        int mixed = blendArgb(base, accentColor, 0.35F);
        return withScaledAlpha(mixed, 0.55F + (strength * 0.35F));
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

    private float clamp(float v, float min, float max)
    {
        return Math.max(min, Math.min(max, v));
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
