package com.lethalfart.ChudWare.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Random;

public class AimAssistHandler
{
    private final AimAssistModule aimAssistModule;
    private final Random random = new Random();
    private EntityPlayer lockedTarget;
    private long nextRetargetAtMs = 0L;
    private long nextJitterUpdateAtMs = 0L;
    private long nextReactionAtMs = 0L;
    private float yawMomentum = 0.0F;
    private float pitchMomentum = 0.0F;
    private float jitterYaw = 0.0F;
    private float jitterPitch = 0.0F;
    private float lastYawStep = 0.0F;
    private float lastPitchStep = 0.0F;

    
    private float targetHeightRatio = 0.9F;

    public AimAssistHandler(AimAssistModule aimAssistModule)
    {
        this.aimAssistModule = aimAssistModule;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!shouldRun(mc)) return;

        long now = System.currentTimeMillis();
        EntityPlayer target = findTarget(mc, now);
        if (target == null)
        {
            decayState();
            return;
        }

        applyRotation(mc, target, now);
    }

    private boolean shouldRun(Minecraft mc)
    {
        return aimAssistModule.isEnabled()
                && mc.thePlayer != null
                && mc.theWorld != null
                && mc.inGameHasFocus
                && mc.currentScreen == null
                && (!aimAssistModule.isOnClickOnly() || Mouse.isButtonDown(0));
    }

    private EntityPlayer findTarget(Minecraft mc, long now)
    {
        if (lockedTarget != null && isTargetStillValid(mc, lockedTarget) && now < nextRetargetAtMs)
        {
            return lockedTarget;
        }

        EntityPlayer best = null;
        double bestScore = Double.MAX_VALUE;

        for (Object obj : mc.theWorld.playerEntities)
        {
            if (!(obj instanceof EntityPlayer)) continue;

            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer || player.isDead || !player.isEntityAlive()) continue;

            double distance = mc.thePlayer.getDistanceToEntity(player);
            if (distance > aimAssistModule.getMaxDistance()) continue;

            float[] desiredAngles = getTargetAngles(mc, player, targetHeightRatio);
            float yawOffset = Math.abs(MathHelper.wrapAngleTo180_float(desiredAngles[0] - mc.thePlayer.rotationYaw));
            if (yawOffset > 110.0F) continue;

            boolean visible = mc.thePlayer.canEntityBeSeen(player);
            float detectionChance = 1.0F;
            if (!visible) detectionChance *= 0.58F;
            if (player.isInvisible()) detectionChance *= 0.42F;
            if (player.getTotalArmorValue() <= 0) detectionChance *= 0.80F;
            if (random.nextFloat() > detectionChance) continue;

            double score = (distance * 2.6D)
                    + (yawOffset * 0.85D)
                    + (visible ? 0.0D : 9.0D)
                    + (player.isInvisible() ? 14.0D : 0.0D)
                    + (random.nextDouble() * 6.0D);

            if (player == lockedTarget) score -= 3.2D;

            if (score < bestScore)
            {
                bestScore = score;
                best = player;
            }
        }

        if (best != null)
        {
            
            if (best != lockedTarget)
            {
                targetHeightRatio = 0.65F + (random.nextFloat() * 0.35F);
            }
            lockedTarget = best;
            nextRetargetAtMs = now + 85L + random.nextInt(180);
        }
        else
        {
            lockedTarget = null;
        }

        return best;
    }

    private void applyRotation(Minecraft mc, EntityPlayer target, long now)
    {
        float[] desiredAngles = getTargetAngles(mc, target, targetHeightRatio);
        float targetYaw = desiredAngles[0];
        float targetPitch = desiredAngles[1];
        float yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(targetPitch - mc.thePlayer.rotationPitch);

        if (now < nextReactionAtMs)
        {
            yawMomentum *= 0.84F;
            pitchMomentum *= 0.84F;
            return;
        }

        
        nextReactionAtMs = now + 45L + random.nextInt(50);

        float maxStep = aimAssistModule.getSpeed() * (0.85F + (random.nextFloat() * 0.30F));
        float smooth = aimAssistModule.getSmoothness() / 100.0F;
        float trackingGain = (0.32F + ((1.0F - smooth) * 0.44F)) * (0.90F + (random.nextFloat() * 0.20F));
        float yawStep = clamp(yawDelta * trackingGain, -maxStep, maxStep);
        float pitchStep = clamp(pitchDelta * trackingGain, -maxStep, maxStep);

        
        if (Math.abs(yawDelta) < 1.0F && random.nextFloat() < 0.55F) yawStep = 0.0F;
        if (Math.abs(pitchDelta) < 0.8F && random.nextFloat() < 0.50F) pitchStep = 0.0F;

        
        if (random.nextFloat() < 0.08F) yawStep = 0.0F;
        if (random.nextFloat() < 0.06F) pitchStep = 0.0F;

        float accelLimit = 0.55F + ((1.0F - smooth) * 1.35F);
        yawStep = clamp(yawStep, lastYawStep - accelLimit, lastYawStep + accelLimit);
        pitchStep = clamp(pitchStep, lastPitchStep - accelLimit, lastPitchStep + accelLimit);
        lastYawStep = yawStep;
        lastPitchStep = pitchStep;

        yawMomentum = (yawMomentum * 0.60F) + (yawStep * (0.36F + (random.nextFloat() * 0.16F)));
        pitchMomentum = (pitchMomentum * 0.60F) + (pitchStep * (0.35F + (random.nextFloat() * 0.15F)));

        if (Math.abs(yawDelta) < 3.0F && random.nextFloat() < 0.18F)
            yawMomentum += Math.signum(yawDelta) * (0.10F + (random.nextFloat() * 0.24F));
        if (Math.abs(pitchDelta) < 2.4F && random.nextFloat() < 0.12F)
            pitchMomentum += Math.signum(pitchDelta) * (0.08F + (random.nextFloat() * 0.18F));

        if (now >= nextJitterUpdateAtMs)
        {
            float jitterScale = 0.02F + ((1.0F - smooth) * 0.20F);
            float yawNoise = ((random.nextFloat() * 2.0F) - 1.0F) * jitterScale;
            float pitchNoise = ((random.nextFloat() * 2.0F) - 1.0F) * (jitterScale * 0.7F);
            jitterYaw = (jitterYaw * 0.64F) + (yawNoise * 0.36F);
            jitterPitch = (jitterPitch * 0.64F) + (pitchNoise * 0.36F);
            nextJitterUpdateAtMs = now + 25L + random.nextInt(36);
        }

        float reactionScale = 0.76F + (random.nextFloat() * 0.30F);
        float yawMove = (yawMomentum * reactionScale) + jitterYaw;
        float pitchMove = (pitchMomentum * reactionScale) + jitterPitch;

        
        float sensitivityStep = getSensitivityStep(mc);
        yawMove = toMouseStep(yawMove, sensitivityStep * (0.5F + random.nextFloat() * 0.5F));
        pitchMove = toMouseStep(pitchMove, sensitivityStep * (0.5F + random.nextFloat() * 0.5F));

        float nextYaw = mc.thePlayer.rotationYaw + yawMove;
        float nextPitch = clamp(mc.thePlayer.rotationPitch + pitchMove, -90.0F, 90.0F);

        mc.thePlayer.rotationYaw = nextYaw;
        mc.thePlayer.rotationPitch = nextPitch;
    }

    private float[] getTargetAngles(Minecraft mc, EntityPlayer target, float heightRatio)
    {
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;

        double targetX = target.posX;
        double targetY = target.posY + (target.getEyeHeight() * heightRatio);
        double targetZ = target.posZ;

        double diffX = targetX - eyeX;
        double diffY = targetY - eyeY;
        double diffZ = targetZ - eyeZ;
        double diffXZ = Math.sqrt((diffX * diffX) + (diffZ * diffZ));

        float targetYaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{ targetYaw, targetPitch };
    }

    private boolean isTargetStillValid(Minecraft mc, EntityPlayer player)
    {
        return player != null
                && player.isEntityAlive()
                && !player.isDead
                && mc.thePlayer.getDistanceToEntity(player) <= aimAssistModule.getMaxDistance();
    }

    private void decayState()
    {
        lockedTarget = null;
        yawMomentum *= 0.55F;
        pitchMomentum *= 0.55F;
        jitterYaw *= 0.45F;
        jitterPitch *= 0.45F;
        lastYawStep *= 0.45F;
        lastPitchStep *= 0.45F;
    }

    private float getSensitivityStep(Minecraft mc)
    {
        float sensitivity = mc.gameSettings.mouseSensitivity;
        float curve = (sensitivity * 0.6F) + 0.2F;
        return curve * curve * curve * 1.2F;
    }

    private float toMouseStep(float angle, float step)
    {
        if (step <= 0.0F) return angle;
        return angle - (angle % step);
    }

    private float clamp(float value, float min, float max)
    {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}