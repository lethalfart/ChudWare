package com.lethalfart.ChudWare.util;




public final class ColorUtil
{
    private ColorUtil()
    {
    }

    


    public static int clamp(int value)
    {
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }

    


    public static int clampArgb(int alpha, int red, int green, int blue)
    {
        return ((clamp(alpha) & 0xFF) << 24)
                | ((clamp(red) & 0xFF) << 16)
                | ((clamp(green) & 0xFF) << 8)
                | (clamp(blue) & 0xFF);
    }
}
