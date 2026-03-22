package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class AutoBridgeModule extends Module
{
    private static final long RELEASE_BUFFER_MS = 90L;
    private static final double EDGE_INSET = 0.24D;
    private static final double CHECK_Y_OFFSET = 0.08D;

    private boolean managedSneak;
    private long sneakReleaseAt;

    public AutoBridgeModule()
    {
        super("Auto Bridge", Category.MOVEMENT);
    }

    @Override
    protected void onEnable()
    {
        managedSneak = false;
        sneakReleaseAt = 0L;
    }

    @Override
    protected void onDisable()
    {
        releaseSneak(Minecraft.getMinecraft());
        managedSneak = false;
        sneakReleaseAt = 0L;
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
            releaseSneak(mc);
            return;
        }

        if (!mc.thePlayer.onGround)
        {
            releaseSneak(mc);
            return;
        }

        boolean atEdge = playerAtVeryEdge(mc);
        updateSneak(mc, atEdge);
    }

    private boolean playerAtVeryEdge(Minecraft mc)
    {
        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        double y = bb.minY - CHECK_Y_OFFSET;

        boolean supportMinMin = hasSupport(mc, bb.minX + EDGE_INSET, y, bb.minZ + EDGE_INSET);
        boolean supportMinMax = hasSupport(mc, bb.minX + EDGE_INSET, y, bb.maxZ - EDGE_INSET);
        boolean supportMaxMin = hasSupport(mc, bb.maxX - EDGE_INSET, y, bb.minZ + EDGE_INSET);
        boolean supportMaxMax = hasSupport(mc, bb.maxX - EDGE_INSET, y, bb.maxZ - EDGE_INSET);

        int supportedCorners = 0;
        if (supportMinMin) supportedCorners++;
        if (supportMinMax) supportedCorners++;
        if (supportMaxMin) supportedCorners++;
        if (supportMaxMax) supportedCorners++;

        return supportedCorners <= 2;
    }

    private boolean hasSupport(Minecraft mc, double x, double y, double z)
    {
        BlockPos below = new BlockPos(x, y, z);
        Block block = mc.theWorld.getBlockState(below).getBlock();
        return !(block instanceof BlockAir) && !(block instanceof BlockLiquid);
    }

    private void updateSneak(Minecraft mc, boolean atEdge)
    {
        if (mc == null || mc.gameSettings == null)
        {
            return;
        }

        if (atEdge)
        {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
            managedSneak = true;
            sneakReleaseAt = System.currentTimeMillis() + RELEASE_BUFFER_MS;
            return;
        }

        if (managedSneak && System.currentTimeMillis() < sneakReleaseAt)
        {
            return;
        }

        releaseSneak(mc);
    }

    private void releaseSneak(Minecraft mc)
    {
        if (!managedSneak || mc == null || mc.gameSettings == null)
        {
            return;
        }

        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        if (!isPhysicalKeyDown(sneakKey))
        {
            KeyBinding.setKeyBindState(sneakKey, false);
        }
        managedSneak = false;
    }

    private boolean isPhysicalKeyDown(int keyCode)
    {
        return keyCode > 0 && Keyboard.isKeyDown(keyCode);
    }
}
