package com.lethalfart.ChudWare.module.impl;

import com.google.common.base.Predicate;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.packet.PacketSendEvent;
import com.lethalfart.ChudWare.eventbus.impl.packet.PacketSentEvent;
import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class ReachModule extends Module
{
    private double reachDistance = 4.2D;
    private int activateTicks = 10;

    private boolean spoofing;
    private double savedX;
    private double savedY;
    private double savedZ;

    public ReachModule()
    {
        super("Reach", Category.COMBAT);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !isEnabled())
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null)
        {
            return;
        }

        Rotation rotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        ReachHit hit = findReachHit(rotation, getReachDistance(), true);
        if (hit == null || hit.entity == null || hit.hitVec == null)
        {
            return;
        }

        mc.pointedEntity = hit.entity;
        mc.objectMouseOver = new MovingObjectPosition(hit.entity, hit.hitVec);
    }

    @EventTarget
    public void onPacketSend(PacketSendEvent event)
    {
        if (!isEnabled() || !(event.getPacket() instanceof C02PacketUseEntity))
        {
            return;
        }

        C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
        if (packet.getAction() != C02PacketUseEntity.Action.ATTACK)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }

        Entity target = packet.getEntityFromWorld(mc.theWorld);
        if (!isAttackableEntity(target) || target == mc.thePlayer)
        {
            return;
        }

        double distance = mc.thePlayer.getDistanceToEntity(target);
        if (distance <= 3.0D || distance > reachDistance)
        {
            return;
        }

        savedX = mc.thePlayer.posX;
        savedY = mc.thePlayer.posY;
        savedZ = mc.thePlayer.posZ;
        spoofing = true;

        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double horizontalDistance = Math.sqrt((dx * dx) + (dz * dz));
        if (horizontalDistance < 0.001D)
        {
            return;
        }

        double pushFraction = (distance - 2.9D) / distance;
        mc.thePlayer.posX += dx * pushFraction;
        mc.thePlayer.posZ += dz * pushFraction;
    }

    @EventTarget
    public void onPacketSent(PacketSentEvent event)
    {
        if (!spoofing || !(event.getPacket() instanceof C02PacketUseEntity))
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null)
        {
            spoofing = false;
            return;
        }

        mc.thePlayer.posX = savedX;
        mc.thePlayer.posY = savedY;
        mc.thePlayer.posZ = savedZ;
        spoofing = false;
    }

    public double getReachDistance()
    {
        return reachDistance;
    }

    public void setReachDistance(double distance)
    {
        this.reachDistance = Math.max(3.0D, Math.min(6.0D, distance));
    }

    public int getActivateTicks()
    {
        return activateTicks;
    }

    public void setActivateTicks(int ticks)
    {
        this.activateTicks = Math.max(1, Math.min(20, ticks));
    }

    private ReachHit findReachHit(Rotation rotation, double reach, boolean checkBlocksFirst)
    {
        Minecraft mc = Minecraft.getMinecraft();
        Entity player = mc.getRenderViewEntity();
        float partialTicks = 1.0F;

        if (player == null || mc.theWorld == null)
        {
            return null;
        }

        MovingObjectPosition blockHit = null;
        if (checkBlocksFirst)
        {
            blockHit = rayTraceBlocks(reach + 2.0D, partialTicks, player.getPositionEyes(partialTicks), rotation, rotation);
        }

        double maxDistance = reach;
        Vec3 eyePos = player.getPositionEyes(partialTicks);
        if (blockHit != null && blockHit.hitVec != null)
        {
            maxDistance = blockHit.hitVec.distanceTo(eyePos);
        }

        Vec3 direction = getDirectionVector(rotation.field2567, rotation.field2565);
        Vec3 rayEnd = eyePos.addVector(
                direction.xCoord * reach,
                direction.yCoord * reach,
                direction.zCoord * reach
        );

        float expand = 1.0F;
        AxisAlignedBB searchBox = player.getEntityBoundingBox()
                .addCoord(direction.xCoord * reach, direction.yCoord * reach, direction.zCoord * reach)
                .expand(expand, expand, expand);

        List<Entity> entityList = mc.theWorld.getEntitiesInAABBexcluding(player, searchBox, new Predicate<Entity>()
        {
            @Override
            public boolean apply(Entity entity)
            {
                return entity != null && entity.isEntityAlive() && !entity.isDead;
            }
        });

        Entity targetEntity = null;
        Vec3 closestHitVec = null;
        double closestDistance = maxDistance;

        for (Entity entity : entityList)
        {
            float border = entity.getCollisionBorderSize();
            AxisAlignedBB entityBoundingBox = entity.getEntityBoundingBox().expand(border, border, border);
            MovingObjectPosition entityHit = entityBoundingBox.calculateIntercept(eyePos, rayEnd);

            if (entityBoundingBox.isVecInside(eyePos))
            {
                if (closestDistance >= 0.0D)
                {
                    targetEntity = entity;
                    closestHitVec = entityHit == null ? eyePos : entityHit.hitVec;
                    closestDistance = 0.0D;
                }
                continue;
            }

            if (entityHit == null)
            {
                continue;
            }

            double hitDistance = eyePos.distanceTo(entityHit.hitVec);
            if (!isValidEntityHit(entity, player, hitDistance, closestDistance))
            {
                continue;
            }

            targetEntity = entity;
            closestHitVec = entityHit.hitVec;
            closestDistance = hitDistance;
        }

        if (targetEntity == null || closestHitVec == null)
        {
            return null;
        }

        double finalDistance = eyePos.distanceTo(closestHitVec);
        if (finalDistance > reach)
        {
            return null;
        }

        if (!(targetEntity instanceof EntityLivingBase) && !(targetEntity instanceof EntityItemFrame))
        {
            return null;
        }

        if (blockHit != null && closestDistance > maxDistance)
        {
            return null;
        }

        return new ReachHit(targetEntity, closestHitVec);
    }

    private MovingObjectPosition rayTraceBlocks(double distance, float partialTicks, Vec3 eyePos, Rotation fromRot, Rotation toRot)
    {
        float yaw = fromRot.field2567 + ((toRot.field2567 - fromRot.field2567) * partialTicks);
        float pitch = fromRot.field2565 + ((toRot.field2565 - fromRot.field2565) * partialTicks);

        Vec3 direction = getDirectionVector(yaw, pitch);
        Vec3 endPos = eyePos.addVector(
                direction.xCoord * distance,
                direction.yCoord * distance,
                direction.zCoord * distance
        );

        return Minecraft.getMinecraft().theWorld.rayTraceBlocks(eyePos, endPos, false, false, true);
    }

    private Vec3 getDirectionVector(float yaw, float pitch)
    {
        float f = MathHelper.cos((-yaw * 0.017453292F) - (float) Math.PI);
        float f1 = MathHelper.sin((-yaw * 0.017453292F) - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    private boolean isValidEntityHit(Entity entity, Entity player, double entityDistance, double closestDistance)
    {
        if (entityDistance < closestDistance || closestDistance <= 0.0D)
        {
            if (!entity.isRiding() || player.canRiderInteract())
            {
                return true;
            }
            return closestDistance <= 0.0D;
        }
        return false;
    }

    private boolean isAttackableEntity(Entity entity)
    {
        return entity instanceof EntityLivingBase || entity instanceof EntityItemFrame;
    }

    private static class ReachHit
    {
        private final Entity entity;
        private final Vec3 hitVec;

        private ReachHit(Entity entity, Vec3 hitVec)
        {
            this.entity = entity;
            this.hitVec = hitVec;
        }
    }

    public static class Rotation
    {
        public float field2567;
        public float field2565;

        public Rotation(float yaw, float pitch)
        {
            this.field2567 = yaw;
            this.field2565 = pitch;
        }

        public Rotation add(Rotation other)
        {
            return new Rotation(this.field2567 + other.field2567, this.field2565 + other.field2565);
        }

        public Rotation subtract(Rotation other)
        {
            return new Rotation(this.field2567 - other.field2567, this.field2565 - other.field2565);
        }
    }
}
