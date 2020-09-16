package com.example.plugin.service;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Set;

import static com.example.plugin.service.Constants.SERVICE_MANAGER_ASM_CLASS_NAME;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author luhaoyu
 */
public class ServiceManagerConstructorVisitor extends MethodVisitor {
    private Set<ServicePair> mServicePairs;

    public ServiceManagerConstructorVisitor(MethodVisitor mv, Set<ServicePair> servicePairSet) {
        super(Opcodes.ASM4, mv);
        mServicePairs = servicePairSet;
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode != RETURN) {
            super.visitInsn(opcode);
            return;
        }
        for (ServicePair servicePair : mServicePairs) {
            add(asmName(servicePair.interfaceName) , asmName(servicePair.implName));
        }
        super.visitInsn(opcode);
    }

    private String asmName(String rawName) {
        return rawName.replace(".", "/");
    }

    private void add(String interfaceName, String implName) {
        MethodVisitor methodVisitor = mv;
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, SERVICE_MANAGER_ASM_CLASS_NAME, "classMap", "Ljava/util/Map;");
//        methodVisitor.visitLdcInsn(Type.getType("L" + "com/gavin/asmdemo/service/ThirdService" + ";"));
        methodVisitor.visitLdcInsn(Type.getType("L" + interfaceName + ";"));
//        methodVisitor.visitLdcInsn(Type.getType("Lcom/gavin/asmdemo/service/ThirdServiceImpl;"));
        methodVisitor.visitLdcInsn(Type.getType("L" + implName + ";"));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitInsn(POP);
    }
}
