package com.example.plugin.service;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Set;

/**
 * @author luhaoyu
 */
public class ServiceManagerClassVisitor extends ClassVisitor implements Opcodes {

    private String mClassName;
    private Set<ServicePair> mServicePairs;

    public ServiceManagerClassVisitor(ClassVisitor cv, Set<ServicePair> servicePairSet) {
        super(Opcodes.ASM5, cv);
        this.mServicePairs = servicePairSet;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        boolean classNameMatch = Constants.SERVICE_MANAGER_ASM_CLASS_NAME.equals(this.mClassName);
        boolean methodNameMatch = Constants.SERVICE_MANAGER_ASM_CONSTRUCTOR.equals(name);
        if (classNameMatch && methodNameMatch) return new ServiceManagerConstructorVisitor(mv, mServicePairs);
        return mv;
    }
}
