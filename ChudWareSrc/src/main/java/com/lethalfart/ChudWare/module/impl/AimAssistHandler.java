package com.lethalfart.ChudWare.module.impl;

import com.sun.jna.platform.win32.User32;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.List;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

public class AimAssistHandler
{
    private static final int VK_LBUTTON = 0x01;
    private static final long AUTOCLICK_INTENT_WINDOW_MS = 150L;

    private final AimAssistModule aimAssistModule;
    private boolean middleDown;
    private EntityPlayer currentTarget;
    private long targetLockUntil;

    public AimAssistHandler(AimAssistModule aimAssistModule)
    {
        this.aimAssistModule = aimAssistModule;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || !mc.inGameHasFocus || mc.currentScreen != null)
        {
            middleDown = false;
            return;
        }

        handleMiddleClickFriend(mc);

        if (!aimAssistModule.isEnabled())
        {
            currentTarget = null;
            return;
        }

        if (aimAssistModule.isBreakBlocks() && isLookingAtSolidBlock(mc))
        {
            return;
        }

        if (aimAssistModule.isWeaponOnly() && !isHoldingWeapon(mc.thePlayer))
        {
            return;
        }

        boolean clicking = isLeftClickActive();
        if (aimAssistModule.isClickAim() && !clicking)
        {
            return;
        }

        Entity target = getEnemy(mc);
        if (target == null)
        {
            currentTarget = null;
            return;
        }

        if (aimAssistModule.isBlatantMode())
        {
            faceEntity(mc.thePlayer, target);
            return;
        }

        double yawDelta = getYawDeltaToEntity(mc.thePlayer, target);
        if (Math.abs(yawDelta) > 0.2D)
        {
            double mainDivisor = 101.0D - ThreadLocalRandom.current().nextDouble(
                    aimAssistModule.getSpeed1() - 4.723847D,
                    aimAssistModule.getSpeed1()
            );
            double baseMove = Math.abs(yawDelta) / Math.max(1.0D, mainDivisor);
            double complimentMove = Math.abs(yawDelta) * (ThreadLocalRandom.current().nextDouble(
                    aimAssistModule.getSpeed2() - 1.47328D,
                    aimAssistModule.getSpeed2() + 2.48293D
            ) / 100.0D) * 0.35D;

            double move = baseMove + complimentMove;
            move = Math.min(Math.abs(yawDelta), move);
            move = Math.max(0.08D, move);

            float adjust = (float) (Math.signum(yawDelta) * move);

            mc.thePlayer.rotationYaw += adjust;
        }
    }

    private void handleMiddleClickFriend(Minecraft mc)
    {
        if (!aimAssistModule.isMiddleClickFriends())
        {
            middleDown = Mouse.isButtonDown(2);
            return;
        }

        boolean currentlyDown = Mouse.isButtonDown(2);
        if (!currentlyDown || middleDown)
        {
            middleDown = currentlyDown;
            return;
        }
        middleDown = true;

        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY)
        {
            return;
        }
        Entity entity = mc.objectMouseOver.entityHit;
        if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer)
        {
            return;
        }
        AimAssistModule.toggleFriend(entity.getName());
    }

    private boolean isLookingAtSolidBlock(Minecraft mc)
    {
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
        {
            return false;
        }
        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null)
        {
            return false;
        }
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block != null && !(block instanceof BlockAir) && !(block instanceof BlockLiquid);
    }

    private Entity getEnemy(Minecraft mc)
    {
        if (currentTarget != null && isValidEnemy(mc, currentTarget))
        {
            if (System.currentTimeMillis() < targetLockUntil)
            {
                return currentTarget;
            }
        }

        List<EntityPlayer> players = mc.theWorld.playerEntities;
        currentTarget = players.stream()
                .filter(player -> isValidEnemy(mc, player))
                .min(Comparator
                        .comparingDouble((EntityPlayer player) -> Math.abs(getYawDeltaToEntity(mc.thePlayer, player)))
                        .thenComparingDouble(player -> mc.thePlayer.getDistanceSqToEntity(player)))
                .orElse(null);

        if (currentTarget != null)
        {
            targetLockUntil = System.currentTimeMillis() + 250L;
        }
        return currentTarget;
    }

    private boolean isValidEnemy(Minecraft mc, EntityPlayer player)
    {
        if (player == mc.thePlayer)
        {
            return false;
        }
        if (aimAssistModule.isIgnoreFriends() && isFriend(player))
        {
            return false;
        }
        if (!aimAssistModule.isAimInvis() && player.isInvisible())
        {
            return false;
        }
        if (mc.thePlayer.getDistanceToEntity(player) >= aimAssistModule.getDistance())
        {
            return false;
        }
        if (!withinFov(mc.thePlayer, player, (int) aimAssistModule.getFov()))
        {
            return false;
        }
        if (aimAssistModule.isIgnoreNaked() && isNaked(player))
        {
            return false;
        }
        return player.isEntityAlive() && !player.isDead;
    }

    private boolean isFriend(EntityPlayer player)
    {
        return player != null && AimAssistModule.isFriend(player.getName());
    }

    private boolean isNaked(EntityPlayer player)
    {
        for (int slot = 0; slot < 4; slot++)
        {
            if (player.getCurrentArmor(slot) != null)
            {
                return false;
            }
        }
        return true;
    }

    private boolean isHoldingWeapon(EntityPlayerSP player)
    {
        if (player.getHeldItem() == null)
        {
            return false;
        }
        return player.getHeldItem().getItem() instanceof ItemSword
                || player.getHeldItem().getItem() instanceof ItemAxe;
    }

    private boolean withinFov(EntityPlayerSP player, Entity entity, int fov)
    {
        double delta = getYawDeltaToEntity(player, entity);
        return Math.abs(delta) <= fov;
    }

    private double getYawDeltaToEntity(EntityPlayerSP player, Entity entity)
    {
        double diffX = entity.posX - player.posX;
        double diffZ = entity.posZ - player.posZ;
        float targetYaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        return MathHelper.wrapAngleTo180_float(targetYaw - player.rotationYaw);
    }

    private void faceEntity(EntityPlayerSP player, Entity entity)
    {
        double diffX = entity.posX - player.posX;
        double diffY = entity.posY + entity.getEyeHeight() - (player.posY + player.getEyeHeight());
        double diffZ = entity.posZ - player.posZ;
        double dist = Math.sqrt((diffX * diffX) + (diffZ * diffZ));
        player.rotationYaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        player.rotationPitch = (float) (-Math.toDegrees(Math.atan2(diffY, dist)));
    }

    private boolean isLeftClickActive()
    {
        if (AutoClickerHandler.hasRecentClickIntent(AUTOCLICK_INTENT_WINDOW_MS))
        {
            return true;
        }
        if (!isWindows())
        {
            return Mouse.isButtonDown(0);
        }
        try
        {
            return (User32.INSTANCE.GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
        }
        catch (Throwable ignored)
        {
            return Mouse.isButtonDown(0);
        }
    }

    private boolean isWindows()
    {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("win");
    }
}
