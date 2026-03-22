package com.lethalfart.ChudWare.asm.hooks;

public class MoveEntityHook
{
    



    public static double[] onMoveEntity(Object entity, double x, double y, double z)
    {

        return new double[]{ x, y, z };
    }
}
