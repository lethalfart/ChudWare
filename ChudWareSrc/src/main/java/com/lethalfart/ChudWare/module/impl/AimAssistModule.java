package com.lethalfart.ChudWare.module.impl;

import com.lethalfart.ChudWare.module.Category;
import com.lethalfart.ChudWare.module.Module;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AimAssistModule extends Module
{
    private static final Set<String> FRIENDS = new HashSet<>();

    private double speed1 = 45.0D;
    private double speed2 = 15.0D;
    private double fov = 90.0D;
    private double distance = 4.5D;
    private boolean clickAim = true;
    private boolean breakBlocks = true;
    private boolean ignoreFriends = true;
    private boolean weaponOnly = false;
    private boolean aimInvis = false;
    private boolean blatantMode = false;
    private boolean ignoreNaked = false;
    private boolean middleClickFriends = true;

    public AimAssistModule()
    {
        super("AimAssist", Category.COMBAT);
    }

    public double getSpeed1()
    {
        return speed1;
    }

    public void setSpeed1(double speed1)
    {
        this.speed1 = clamp(speed1, 5.0D, 100.0D);
    }

    public double getSpeed2()
    {
        return speed2;
    }

    public void setSpeed2(double speed2)
    {
        this.speed2 = clamp(speed2, 2.0D, 97.0D);
    }

    public double getFov()
    {
        return fov;
    }

    public void setFov(double fov)
    {
        this.fov = clamp(fov, 15.0D, 360.0D);
    }

    public double getDistance()
    {
        return distance;
    }

    public void setDistance(double distance)
    {
        this.distance = clamp(distance, 1.0D, 10.0D);
    }

    public boolean isClickAim()
    {
        return clickAim;
    }

    public void setClickAim(boolean clickAim)
    {
        this.clickAim = clickAim;
    }

    public boolean isBreakBlocks()
    {
        return breakBlocks;
    }

    public void setBreakBlocks(boolean breakBlocks)
    {
        this.breakBlocks = breakBlocks;
    }

    public boolean isIgnoreFriends()
    {
        return ignoreFriends;
    }

    public void setIgnoreFriends(boolean ignoreFriends)
    {
        this.ignoreFriends = ignoreFriends;
    }

    public boolean isWeaponOnly()
    {
        return weaponOnly;
    }

    public void setWeaponOnly(boolean weaponOnly)
    {
        this.weaponOnly = weaponOnly;
    }

    public boolean isAimInvis()
    {
        return aimInvis;
    }

    public void setAimInvis(boolean aimInvis)
    {
        this.aimInvis = aimInvis;
    }

    public boolean isBlatantMode()
    {
        return blatantMode;
    }

    public void setBlatantMode(boolean blatantMode)
    {
        this.blatantMode = blatantMode;
    }

    public boolean isIgnoreNaked()
    {
        return ignoreNaked;
    }

    public void setIgnoreNaked(boolean ignoreNaked)
    {
        this.ignoreNaked = ignoreNaked;
    }

    public boolean isMiddleClickFriends()
    {
        return middleClickFriends;
    }

    public void setMiddleClickFriends(boolean middleClickFriends)
    {
        this.middleClickFriends = middleClickFriends;
    }

    public static boolean isFriend(String name)
    {
        return name != null && FRIENDS.contains(name.toLowerCase());
    }

    public static boolean toggleFriend(String name)
    {
        if (name == null || name.trim().isEmpty())
        {
            return false;
        }
        String key = name.toLowerCase();
        if (FRIENDS.contains(key))
        {
            FRIENDS.remove(key);
            return false;
        }
        FRIENDS.add(key);
        return true;
    }

    public static Set<String> getFriends()
    {
        return Collections.unmodifiableSet(FRIENDS);
    }

    public static void setFriends(Iterable<String> names)
    {
        FRIENDS.clear();
        if (names == null)
        {
            return;
        }
        for (String name : names)
        {
            if (name != null && !name.trim().isEmpty())
            {
                FRIENDS.add(name.toLowerCase());
            }
        }
    }

    private double clamp(double value, double min, double max)
    {
        return value < min ? min : (value > max ? max : value);
    }
}
