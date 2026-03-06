package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.eventbus.impl.move.MoveEvent;
import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class AutoBridgeModule extends Module 
{
    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasAtEdge = false;
    private Robot robot;
    private boolean shiftPressed = false;

    public AutoBridgeModule()
    {
        super("AutoBridge", Category.MOVEMENT);
    }

    @Override
    protected void onEnable()
    {
        MinecraftForge.EVENT_BUS.register(this);
        wasAtEdge = false;
        try {
            robot = new Robot();
        } catch (Exception e) {
        }
        shiftPressed = false;
        try {
            Field pressedField = net.minecraft.client.settings.KeyBinding.class.getDeclaredField("pressed");
            pressedField.setAccessible(true);
            pressedField.set(mc.gameSettings.keyBindSneak, false);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onDisable()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
        if (shiftPressed && robot != null) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
            shiftPressed = false;
        }
        try {
            Field pressedField = net.minecraft.client.settings.KeyBinding.class.getDeclaredField("pressed");
            pressedField.setAccessible(true);
            pressedField.set(mc.gameSettings.keyBindSneak, false);
        } catch (Exception e) {
        }
    }

    @SubscribeEvent
    public void onMove(MoveEvent event)
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }

        EntityPlayer player = mc.thePlayer;

        if (!player.onGround)
        {
            return;
        }

        if (!isMovingBackwards(player, event.x, event.z))
        {
            return;
        }

        double x = event.x;
        double z = event.z;
        double step = 0.05D;

        boolean atEdgeCurrent = (x != 0.0D && hasNoGroundPredicted(player, x, 0.0D)) || (z != 0.0D && hasNoGroundPredicted(player, 0.0D, z)) || (x != 0.0D && z != 0.0D && hasNoGroundPredicted(player, x, z));

        wasAtEdge = atEdgeCurrent;

        boolean shouldSneak = atEdgeCurrent;
        if (shouldSneak && !shiftPressed && robot != null) {
            robot.keyPress(KeyEvent.VK_SHIFT);
            shiftPressed = true;
        } else if (!shouldSneak && shiftPressed && robot != null) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
            shiftPressed = false;
        }
        try {
            Field pressedField = net.minecraft.client.settings.KeyBinding.class.getDeclaredField("pressed");
            pressedField.setAccessible(true);
            pressedField.set(mc.gameSettings.keyBindSneak, shouldSneak);
        } catch (Exception e) {
        }

        if (shouldSneak) {
            while (x != 0.0D && hasNoGround(player, x, 0.0D))
            {
                if (x < step && x >= -step)
                {
                    x = 0.0D;
                }
                else if (x > 0.0D)
                {
                    x -= step;
                }
                else
                {
                    x += step;
                }
            }

            while (z != 0.0D && hasNoGround(player, 0.0D, z))
            {
                if (z < step && z >= -step)
                {
                    z = 0.0D;
                }
                else if (z > 0.0D)
                {
                    z -= step;
                }
                else
                {
                    z += step;
                }
            }

            while (x != 0.0D && z != 0.0D && hasNoGround(player, x, z))
            {
                if (x < step && x >= -step)
                {
                    x = 0.0D;
                }
                else if (x > 0.0D)
                {
                    x -= step;
                }
                else
                {
                    x += step;
                }

                if (z < step && z >= -step)
                {
                    z = 0.0D;
                }
                else if (z > 0.0D)
                {
                    z -= step;
                }
                else
                {
                    z += step;
                }
            }
        }

        event.x = x;
        event.z = z;
    }

    private boolean hasNoGround(EntityPlayer player, double x, double z)
    {
        AxisAlignedBB box = player.getEntityBoundingBox().offset(x, -1.0D, z);
        return mc.theWorld.getCollidingBoundingBoxes(player, box).isEmpty();
    }

    private boolean hasNoGroundPredicted(EntityPlayer player, double x, double z)
    {
        double ticks = 1.6D;
        double predictedX = player.posX + x * ticks;
        double predictedZ = player.posZ + z * ticks;
        AxisAlignedBB box = player.getEntityBoundingBox().offset(predictedX - player.posX, -1.0D, predictedZ - player.posZ);
        return mc.theWorld.getCollidingBoundingBoxes(player, box).isEmpty();
    }

    private boolean isMovingBackwards(EntityPlayer player, double x, double z)
    {
        float yaw = player.rotationYaw;
        double yawRad = Math.toRadians(yaw);
        
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        
        double dotProduct = x * forwardX + z * forwardZ;
        
        return dotProduct < 0;
    }
}