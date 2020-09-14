package com.gavin.plugin.lifecycle;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author gavin
 * @date 2019/2/18
 * lifecycle class visitor
 */
public class SettingManagerClassVisitor extends ClassVisitor implements Opcodes {

    private String mClassName;

    public SettingManagerClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        //System.out.println("LifecycleClassVisitor : visitMethod : " + name);
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if ("com/gavin/asmdemo/ServiceManager".equals(this.mClassName)) {
            if ("<init>".equals(name) ) {
                //处理onCreate
                System.out.println("SettingManagerClassVisitor : change method ----> " + name);
                return new SettingsManagerConstructorMethodVisitor(mv);
            }
        }
        return mv;
    }
}
