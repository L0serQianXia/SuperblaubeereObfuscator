package me.superblaubeere27.jobf.processors.flowObfuscation;

import me.superblaubeere27.jobf.ProcessorCallback;
import org.objectweb.asm.Opcodes;
import me.superblaubeere27.jobf.utils.NodeUtils;
import org.objectweb.asm.tree.*;

import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

/**
 * @description: Stole from Radon
 * @author: ItzSomebody (https://github.com/ItzSomebody/)
 * @create: 2021/12/26 19:08
 **/
public class FakeTryCatchBlocks {
    private static final String[] HANDLER_NAMES = {
            RuntimeException.class.getName().replace('.', '/'),
            LinkageError.class.getName().replace('.', '/'),
            Error.class.getName().replace('.', '/'),
            Exception.class.getName().replace('.', '/'),
            Throwable.class.getName().replace('.', '/'),
            IllegalArgumentException.class.getName().replace('.', '/'),
            IllegalStateException.class.getName().replace('.', '/'),
            IllegalAccessError.class.getName().replace('.', '/'),
            InvocationTargetException.class.getName().replace('.', '/'),
            IOException.class.getName().replace('.', '/'),
            IOError.class.getName().replace('.', '/'),
    };

    static void fakeTryCatchBlocks(ProcessorCallback callback, MethodNode method) {
        if ("<init>".equals(method.name)) {
            return;
        }

        Random rand = new Random();
        int freeSize = NodeUtils.getMethodFreeSize(method);
        String handlerName = HANDLER_NAMES[rand.nextInt(HANDLER_NAMES.length)];

        for (AbstractInsnNode abstractInsnNode : method.instructions) {
            if (freeSize > 10000 && isVaild(abstractInsnNode) && rand.nextInt(10) > 6) {
                LabelNode start = new LabelNode();
                LabelNode end = new LabelNode();
                LabelNode handler = new LabelNode();
                LabelNode handlerEnd = new LabelNode();

                InsnList list = new InsnList();
                list.add(handler);
                list.add(new InsnNode(Opcodes.DUP));
                // the method does not exist, so that the programme can be crashed if some exception happens
                list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, handlerName, "a", "()V", false));
                list.add(new InsnNode(Opcodes.ATHROW));
                list.add(handlerEnd);

                method.instructions.insertBefore(abstractInsnNode, start);
                method.instructions.insert(abstractInsnNode, list);
                method.instructions.insert(abstractInsnNode, new JumpInsnNode(Opcodes.GOTO, handlerEnd));
                method.instructions.insert(abstractInsnNode, end);

                method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, handlerName));

                freeSize -= 15;
            }
        }
        callback.setForceComputeFrames();
    }

    private static boolean isVaild(AbstractInsnNode abstractInsnNode) {
        return NodeUtils.isInsnValid(abstractInsnNode) && abstractInsnNode.getOpcode() != Opcodes.RETURN;
    }
}
