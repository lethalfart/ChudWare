package com.lethalfart.ChudWare.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class ChudWareTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (transformedName.equals("net.minecraft.client.entity.EntityPlayerSP"))
        {
            return transformEntityPlayerSP(basicClass);
        }
        return basicClass;
    }

    private byte[] transformEntityPlayerSP(byte[] basicClass)
    {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patchedPreMotion  = false;
        boolean patchedAlwaysTick = false;

        for (MethodNode method : classNode.methods)
        {
            if (!patchedPreMotion)
            {
                boolean isWalking = method.name.equals("onUpdateWalkingPlayer")
                        || method.name.equals("func_110307_c")
                        || method.name.equals("t_");
                if (isWalking && method.desc.equals("()V"))
                {
                    InsnList toInsert = new InsnList();
                    toInsert.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/lethalfart/ChudWare/asm/hooks/PreMotionHook",
                            "onPreMotion",
                            "()V",
                            false
                    ));
                    method.instructions.insert(toInsert);
                    patchedPreMotion = true;
                    System.out.println("[ChudWare] Patched onUpdateWalkingPlayer -> onPreMotion");
                }
            }

            if (!patchedAlwaysTick)
            {
                boolean isEntityUpdate = method.name.equals("onEntityUpdate")
                        || method.name.equals("func_70071_h_")
                        || method.name.equals("p");
                if (isEntityUpdate && method.desc.equals("()V"))
                {
                    InsnList toInsert = new InsnList();
                    toInsert.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/lethalfart/ChudWare/asm/hooks/PreMotionHook",
                            "onPreMotionAlways",
                            "()V",
                            false
                    ));
                    method.instructions.insert(toInsert);
                    patchedAlwaysTick = true;
                    System.out.println("[ChudWare] Patched onEntityUpdate -> onPreMotionAlways");
                }
            }

            if (patchedPreMotion && patchedAlwaysTick) break;
        }

        if (!patchedPreMotion)
            System.out.println("[ChudWare] WARNING: Could not find onUpdateWalkingPlayer");
        if (!patchedAlwaysTick)
            System.out.println("[ChudWare] WARNING: Could not find onEntityUpdate");

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
