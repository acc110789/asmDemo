package com.gavin.plugin.lifecycle;

import com.android.annotations.NonNull;
import com.android.build.api.transform.*;
import com.android.build.gradle.internal.pipeline.TransformManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

class SettingManagerTransform extends Transform {

    private ServiceCollector collector;
    private Map<String, SettingPair> kaptSettingPairs = new HashMap<>();
    private Set<String> configSimpleClassNameSet = new HashSet<>();

    private Map<String, SettingPair> finalSettingPairs = new HashMap<>();

    public SettingManagerTransform(Project project) {
        this.collector = new ServiceCollector(project);
    }

    @Override
    public String getName() {
        return "LifecyclePlugin";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(@NonNull TransformInvocation transformInvocation) throws IOException {
        long startTime = System.currentTimeMillis();
        Logger.log("--------------- LifecyclePlugin visit start --------------- ");

        kaptSettingPairs.clear();
        Map<String, SettingPair> configSettingPair = collector.collectSettingPair();
        this.kaptSettingPairs.putAll(configSettingPair);
        Logger.log("kaptSettingPairs: " + kaptSettingPairs);

        configSimpleClassNameSet.clear();
        configSimpleClassNameSet.addAll(Utils.getSimpleClassNameSet(configSettingPair));
        Logger.log("configSimpleClassNameSet: " + configSimpleClassNameSet);

        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        //删除之前的输出
        if (outputProvider != null) outputProvider.deleteAll();

        //遍历inputs
        for (TransformInput input : inputs) {
            //遍历directoryInputs
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                handleDirectoryInput(directoryInput, outputProvider);
            }

            //遍历jarInputs
            for (JarInput jarInput : input.getJarInputs()) {
                handleJarInputs(jarInput, outputProvider);
            }
        }

        logFinalSettingPair();

        long cost = (System.currentTimeMillis() - startTime) / 1000;
        Logger.log("--------------- LifecyclePlugin visit end --------------- ");
        Logger.log("LifecyclePlugin cost ： " + cost + " s");
    }

    private void logFinalSettingPair() {
        Logger.log("finalSettingPairs:  " + finalSettingPairs);
    }

    /**
     * 处理文件目录下的class文件
     */
    void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) throws IOException {
        File parent = directoryInput.getFile();
        Logger.log("----------- directory parent file <" + parent.getAbsolutePath() + "> -----------");

        //是否是目录
        if (parent.isDirectory()) {
            //列出目录所有文件（包含子文件夹，子文件夹内文件）
            Utils.eachFileRecurse(parent, new FileHandler() {
                @Override
                public void handleFile(File file) throws IOException {
                    String name = file.getName();
                    if (!checkSimpleClassName(name)) return;

                    byte[] bytes = Utils.getBytes(file);
                    checkDetailClassName(bytes);

//                    handleSettingManagerClass();
                }
            });
        }
        //处理完输入文件之后，要把输出给下一个任务
        File dest = outputProvider.getContentLocation(directoryInput.getName(),
                directoryInput.getContentTypes(), directoryInput.getScopes(),
                Format.DIRECTORY);
        FileUtils.copyDirectory(directoryInput.getFile(), dest);
    }

    /**
     * 处理Jar中的class文件
     */
    void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider) throws IOException {
        if (jarInput.getFile().getAbsolutePath().endsWith(".jar")) {
            //重名名输出文件,因为可能同名,会覆盖
            String jarName = jarInput.getName();
            String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4);
            }
            JarFile jarFile = new JarFile(jarInput.getFile());
            Enumeration enumeration = jarFile.entries();
            File tmpFile = new File(jarInput.getFile().getParent() + File.separator + "classes_temp.jar");
            //避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));
            //用于保存
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                //插桩class
                if (checkSimpleClassName(getSimpleClassNameForJarEntryName(entryName))) {
                    //class文件处理
                    Logger.log("----------- deal with jar class file <" + entryName + "> -----------");
                    jarOutputStream.putNextEntry(zipEntry);
                    byte[] bytes = IOUtils.toByteArray(inputStream);

                    checkDetailClassName(bytes);

//                    ClassReader classReader = new ClassReader(bytes);
//                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
//                    ClassVisitor cv = new SettingManagerClassVisitor(classWriter);
//                    classReader.accept(cv, EXPAND_FRAMES);
//                    byte[] code = classWriter.toByteArray();
//                    jarOutputStream.write(code);

                    jarOutputStream.write(bytes);
                } else {
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }
            //结束
            jarOutputStream.close();
            jarFile.close();
            File dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
            FileUtils.copyFile(tmpFile, dest);
            tmpFile.delete();
        }
    }

    private String getSimpleClassNameForJarEntryName(String jarEntryName) {
        int nameDividerIndex = jarEntryName.lastIndexOf("/");
        if (nameDividerIndex < 0) return jarEntryName;
        return jarEntryName.substring(nameDividerIndex + 1);
    }

    private boolean checkSimpleClassName(String name) {
        //只处理需要的class文件
        return name.endsWith(".class") && configSimpleClassNameSet.contains(name);
    }

//    private void handleSettingManagerClass() {
//        ClassReader classReader = new ClassReader(bytes);
//
//        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
//        ClassVisitor classVisitor = new SettingManagerClassVisitor(classWriter);
//        classReader.accept(classVisitor, EXPAND_FRAMES);
//        byte[] code = classWriter.toByteArray();
//        FileOutputStream fos = new FileOutputStream(
//                file.getParentFile().getAbsolutePath() + File.separator + name);
//        fos.write(code);
//        fos.close();
//    }


    private void checkDetailClassName(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        String rawClassName = classReader.getClassName();
        Logger.log("checkDetailClassName getClassName: " + rawClassName);
        String className = rawClassName.replace("/", ".");

        for (Map.Entry<String, SettingPair> entry: kaptSettingPairs.entrySet()) {
            String key = entry.getKey();
            SettingPair settingPair = entry.getValue();

            if (settingPair.interfaceName.equals(className)) {
                SettingPair finalSettingPair = this.finalSettingPairs.get(key);
                if (finalSettingPair == null) {
                    finalSettingPair = new SettingPair();
                    this.finalSettingPairs.put(key, finalSettingPair);
                }
                finalSettingPair.interfaceName = className;

            }

            if (settingPair.implName.equals(className)) {
                SettingPair finalSettingPair = this.finalSettingPairs.get(key);
                if (finalSettingPair == null) {
                    finalSettingPair = new SettingPair();
                    this.finalSettingPairs.put(key, finalSettingPair);
                }
                finalSettingPair.implName = className;
            }
        }
        Logger.log("checkDetailClassName finalSettingPairs: " + finalSettingPairs);
    }
}
