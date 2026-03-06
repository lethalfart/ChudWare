package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.ChudWare;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import com.lethalfart.ChudWare.eventbus.annotations.EventTarget;
import com.lethalfart.ChudWare.eventbus.impl.misc.TickEvent;

public class ReachHandler
{
    private static final double VANILLA_REACH = 3.0;

    private final ReachModule reachModule;

    
    private double lockedReach = VANILLA_REACH;
    private boolean wasAttackDown = false;

    public ReachHandler(ReachModule reachModule)
    {
        this.reachModule = reachModule;
        ChudWare.EVENT_MANAGER.register(this);
    }

    @EventTarget
    public void onTick(TickEvent event)
    {
        if (!reachModule.isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!isValidState(mc)) return;

        boolean attackDown = mc.gameSettings.keyBindAttack.isKeyDown();

        
        if (attackDown && !wasAttackDown)
        {
            lockedReach = reachModule.getReach();
        }
        wasAttackDown = attackDown;

        if (!attackDown) return;

        
        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
        {
            BlockPos p = mc.objectMouseOver.getBlockPos();
            if (p != null && mc.theWorld.getBlockState(p).getBlock() != Blocks.air)
                return;
        }

        
        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)
            return;

        
        if (lockedReach <= VANILLA_REACH) return;

        Object[] result = getEntity(mc, lockedReach);
        if (result == null) return;

        Entity entity = (Entity) result[0];
        Vec3 hitVec = (Vec3) result[1];

        mc.objectMouseOver = new MovingObjectPosition(entity, hitVec);
        mc.pointedEntity = entity;
    }

    private Object[] getEntity(Minecraft mc, double reach)
    {
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null) return null;

        Vec3 eyePos = viewer.getPositionEyes(1.0F);
        Vec3 lookVec = viewer.getLook(1.0F);
        Vec3 endPos = eyePos.addVector(
                lookVec.xCoord * reach,
                lookVec.yCoord * reach,
                lookVec.zCoord * reach
        );

        Entity hitEntity = null;
        Vec3 hitVec = null;
        double bestDist = reach;

        for (Object obj : mc.theWorld.loadedEntityList)
        {
            Entity entity = (Entity) obj;
            if (!(entity instanceof EntityLivingBase)) continue;
            if (entity == viewer) continue;
            if (!entity.canBeCollidedWith()) continue;
            if (!entity.isEntityAlive()) continue;

            
            AxisAlignedBB bb = entity.getEntityBoundingBox();
            MovingObjectPosition intercept = bb.calculateIntercept(eyePos, endPos);

            if (intercept != null)
            {
                double dist = eyePos.distanceTo(intercept.hitVec);

                
                if (dist <= VANILLA_REACH) continue;

                if (dist < bestDist)
                {
                    bestDist = dist;
                    hitEntity = entity;
                    hitVec = intercept.hitVec;
                }
            }
        }

        if (hitEntity == null) return null;
        return new Object[]{ hitEntity, hitVec };
    }

    private boolean isValidState(Minecraft mc)
    {
        return mc != null
                && mc.thePlayer != null
                && mc.theWorld != null
                && mc.inGameHasFocus
                && mc.currentScreen == null;
    }
}