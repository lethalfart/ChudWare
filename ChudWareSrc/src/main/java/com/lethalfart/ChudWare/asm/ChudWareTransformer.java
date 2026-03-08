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
        if (transformedName.equals("net.minecraft.entity.Entity"))
        {
            return transformEntity(basicClass);
        }
        if (transformedName.equals("net.minecraft.client.entity.EntityPlayerSP"))
        {
            return transformEntityPlayerSP(basicClass);
        }
        return basicClass;
    }

    private byte[] transformEntity(byte[] basicClass)
    {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        for (MethodNode method : classNode.methods)
        {
            boolean isMoveEntity = (method.name.equals("moveEntity")
                    || method.name.equals("func_70091_d")
                    || method.name.equals("d"))
                    && method.desc.equals("(DDD)V");
            if (isMoveEntity)
            {
                InsnList toInsert = new InsnList();
                toInsert.add(new VarInsnNode(Opcodes.ALOAD, 0));
                toInsert.add(new VarInsnNode(Opcodes.DLOAD, 1));
                toInsert.add(new VarInsnNode(Opcodes.DLOAD, 3));
                toInsert.add(new VarInsnNode(Opcodes.DLOAD, 5));
                toInsert.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/lethalfart/ChudWare/asm/hooks/MoveEntityHook",
                        "onMoveEntity",
                        "(Ljava/lang/Object;DDD)[D",
                        false
                ));

                toInsert.add(new InsnNode(Opcodes.DUP));
                toInsert.add(new InsnNode(Opcodes.ICONST_0));
                toInsert.add(new InsnNode(Opcodes.DALOAD));
                toInsert.add(new VarInsnNode(Opcodes.DSTORE, 1));

                toInsert.add(new InsnNode(Opcodes.DUP));
                toInsert.add(new InsnNode(Opcodes.ICONST_1));
                toInsert.add(new InsnNode(Opcodes.DALOAD));
                toInsert.add(new VarInsnNode(Opcodes.DSTORE, 3));

                toInsert.add(new InsnNode(Opcodes.ICONST_2));
                toInsert.add(new InsnNode(Opcodes.DALOAD));
                toInsert.add(new VarInsnNode(Opcodes.DSTORE, 5));

                method.instructions.insert(toInsert);
                break;
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private byte[] transformEntityPlayerSP(byte[] basicClass)
    {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);


        System.out.println("[ChudWare] EntityPlayerSP methods:");
        for (MethodNode method : classNode.methods)
        {
            System.out.println("[ChudWare]   " + method.name + " " + method.desc);
        }

        boolean patched = false;
        for (MethodNode method : classNode.methods)
        {
            boolean isTarget = method.name.equals("onUpdateWalkingPlayer")
                    || method.name.equals("func_110307_c")
                    || method.name.equals("a");


            boolean isDescMatch = method.desc.equals("()V")
                    && !method.name.equals("<init>")
                    && !method.name.equals("onUpdate")
                    && !method.name.equals("func_70071_h_")
                    && !method.name.equals("onLivingUpdate")
                    && !method.name.equals("func_70636_d")
                    && !method.name.equals("onEntityUpdate")
                    && !method.name.equals("func_70071_h_")
                    && !method.name.equals("pushOutOfBlocks")
                    && !method.name.equals("tick");

            if (!isTarget) continue;

            System.out.println("[ChudWare] Patching EntityPlayerSP method: " + method.name + " " + method.desc);

            InsnList toInsert = new InsnList();
            toInsert.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "com/lethalfart/ChudWare/asm/hooks/PreMotionHook",
                    "onPreMotion",
                    "()V",
                    false
            ));
            method.instructions.insert(toInsert);
            patched = true;
            break;
        }

        if (!patched)
        {
            System.out.println("[ChudWare] WARNING: Could not find onUpdateWalkingPlayer in EntityPlayerSP");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
