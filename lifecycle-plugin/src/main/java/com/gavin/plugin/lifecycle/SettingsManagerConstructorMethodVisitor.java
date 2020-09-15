package com.gavin.plugin.lifecycle;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author gavin
 * @date 2019/2/19
 */
public class SettingsManagerConstructorMethodVisitor extends MethodVisitor {
    private Set<SettingPair> mSettingPairs;

    public SettingsManagerConstructorMethodVisitor(MethodVisitor mv, Set<SettingPair> settingPairSet) {
        super(Opcodes.ASM4, mv);
        mSettingPairs = settingPairSet;
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode != RETURN) {
            super.visitInsn(opcode);
            return;
        }
        for (SettingPair settingPair : mSettingPairs) {
            add(asmName(settingPair.interfaceName) , asmName(settingPair.implName));
        }
        super.visitInsn(opcode);
    }

    private String asmName(String rawName) {
        return rawName.replace(".", "/");
    }

    private void addTwo() {
        add("com/gavin/asmdemo/service/TwoService", "com/gavin/asmdemo/service/TwoServiceImpl");
    }

    private void addThird() {
        add("com/gavin/asmdemo/service/ThirdService", "com/gavin/asmdemo/service/ThirdServiceImpl");
    }

    private void add(String interfaceName, String implName) {
        MethodVisitor methodVisitor = mv;
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, "com/gavin/asmdemo/ServiceManager", "classMap", "Ljava/util/Map;");
//        methodVisitor.visitLdcInsn(Type.getType("L" + "com/gavin/asmdemo/service/ThirdService" + ";"));
        methodVisitor.visitLdcInsn(Type.getType("L" + interfaceName + ";"));
//        methodVisitor.visitLdcInsn(Type.getType("Lcom/gavin/asmdemo/service/ThirdServiceImpl;"));
        methodVisitor.visitLdcInsn(Type.getType("L" + implName + ";"));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitInsn(POP);
    }
}
