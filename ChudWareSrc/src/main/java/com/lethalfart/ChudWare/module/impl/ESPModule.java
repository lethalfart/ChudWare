package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;
import com.lethalfart.ChudWare.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

public class ESPModule extends Module
{
    private int red = 168;
    private int green = 62;
    private int blue = 94;
    private int alpha = 50;

    private boolean targetPlayers = true;
    private boolean targetAnimals = false;
    private boolean targetMobs = false;
    private boolean targetPassives = false;

    public ESPModule()
    {
        super("ESP", Category.VISUAL);
    }

    public int getRed()
    {
        return red;
    }

    public void setRed(int red)
    {
        this.red = ColorUtil.clamp(red);
    }

    public int getGreen()
    {
        return green;
    }

    public void setGreen(int green)
    {
        this.green = ColorUtil.clamp(green);
    }

    public int getBlue()
    {
        return blue;
    }

    public void setBlue(int blue)
    {
        this.blue = ColorUtil.clamp(blue);
    }

    public int getAlpha()
    {
        return alpha;
    }

    public void setAlpha(int alpha)
    {
        this.alpha = Math.max(50, Math.min(255, alpha));
    }

    public boolean isTargetPlayers()
    {
        return targetPlayers;
    }

    public void setTargetPlayers(boolean targetPlayers)
    {
        this.targetPlayers = targetPlayers;
    }

    public boolean isTargetAnimals()
    {
        return targetAnimals;
    }

    public void setTargetAnimals(boolean targetAnimals)
    {
        this.targetAnimals = targetAnimals;
    }

    public boolean isTargetMobs()
    {
        return targetMobs;
    }

    public void setTargetMobs(boolean targetMobs)
    {
        this.targetMobs = targetMobs;
    }

    public boolean isTargetPassives()
    {
        return targetPassives;
    }

    public void setTargetPassives(boolean targetPassives)
    {
        this.targetPassives = targetPassives;
    }

    public boolean isValid(EntityLivingBase entity)
    {
        Minecraft mc = Minecraft.getMinecraft();
        return isValidType(entity)
                && entity.isEntityAlive()
                && !entity.isInvisible()
                && entity != mc.thePlayer;
    }

    public boolean isValidType(Entity entity)
    {
        return (targetPlayers && entity instanceof EntityPlayer)
                || (targetMobs && (entity instanceof EntityMob || entity instanceof EntitySlime))
                || (targetPassives && (entity instanceof EntityVillager || entity instanceof EntityGolem))
                || (targetAnimals && entity instanceof EntityAnimal);
    }
}
