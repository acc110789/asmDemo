package com.gavin.plugin.lifecycle;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author gavin
 * @date 2019/2/19
 */
public class SettingsManagerConstructorMethodVisitor extends MethodVisitor {

    public SettingsManagerConstructorMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM4, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode != RETURN) {
            super.visitInsn(opcode);
            return;
        }
        addTwo();
        addThird();
        super.visitInsn(opcode);
    }

    private void addTwo() {
        MethodVisitor methodVisitor = mv;
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, "com/gavin/asmdemo/ServiceManager", "classMap", "Ljava/util/Map;");
        methodVisitor.visitLdcInsn(Type.getType("Lcom/gavin/asmdemo/service/TwoService;"));
        methodVisitor.visitLdcInsn(Type.getType("Lcom/gavin/asmdemo/service/TwoServiceImpl;"));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitInsn(POP);
    }

    private void addThird() {
        MethodVisitor methodVisitor = mv;
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, "com/gavin/asmdemo/ServiceManager", "classMap", "Ljava/util/Map;");
        methodVisitor.visitLdcInsn(Type.getType("Lcom/gavin/asmdemo/service/ThirdService;"));
        methodVisitor.visitLdcInsn(Type.getType("Lcom/gavin/asmdemo/service/ThirdServiceImpl;"));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitInsn(POP);
    }
}
