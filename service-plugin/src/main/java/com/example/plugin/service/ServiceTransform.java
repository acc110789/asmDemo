package com.example.plugin.service;

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

import static com.example.plugin.service.Constants.JAR_POSTFIX;
import static com.example.plugin.service.Constants.SERVICE_MANAGER_FULL_NAME;
import static com.example.plugin.service.Constants.SERVICE_MANAGER_SIMPLE_CLASS_NAME;
import static com.example.plugin.service.Constants.TMP_JAR_FILE_NAME;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

class ServiceTransform extends Transform {

    private ServiceCollector collector;

    //通过kapt收集到的所有Service和ServiceImpl信息,
    //key: Impl, value: Interface
    private Map<String, String> kaptServiceMap = new HashMap<>();
    private Set<String> implServiceSimpleClassNameSet = new HashSet<>();

    private Map<String, String> finalServicePairs = new HashMap<>();

    private byte[] settingManagerClassBytes = null;

    public ServiceTransform(Project project) {
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
        Logger.log("---------------visit start --------------- ");

        this.kaptServiceMap.clear();
        Map<String, String> kaptServiceMap = collector.getAllServicePair();
        this.kaptServiceMap.putAll(kaptServiceMap);
        Logger.log("kaptServicePairs: " + this.kaptServiceMap);

        implServiceSimpleClassNameSet.clear();
        implServiceSimpleClassNameSet.addAll(Utils.getImplSimpleClassNameSet(kaptServiceMap));
        Logger.log("kaptSimpleClassNameSet: " + implServiceSimpleClassNameSet);

        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        if (outputProvider != null) outputProvider.deleteAll();

        File lastDirectoryDest = null;
        for (TransformInput input : inputs) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                lastDirectoryDest = handleDirectoryInput(directoryInput, outputProvider);
            }
            for (JarInput jarInput : input.getJarInputs()) {
                handleJarInputs(jarInput, outputProvider);
            }
        }

        generateServiceManagerClass(lastDirectoryDest);
        logFinalServicePair();

        long cost = (System.currentTimeMillis() - startTime) / 1000;
        Logger.log("---------------visit end ----------------- ");
        Logger.log(" cost ： " + cost + " s");
    }

    private void logFinalServicePair() {
        Logger.log("finalServicePairs:  " + finalServicePairs);
    }

    private void generateServiceManagerClass(File destination) throws IOException {
        byte[] bytes = settingManagerClassBytes;
        if (bytes == null) return;

        ClassReader classReader = new ClassReader(bytes);
        //rawClassName:  com/example/gavin/asmdemo/service/SettingManager
        String rawClassName = classReader.getClassName();
        String filePath = rawClassName;
        if (!"/".equals(File.separator)) {
            filePath = rawClassName.replace("/", File.separator);
        }
        filePath = filePath + ".class";
        File file = new File(destination, filePath);
        file.getParentFile().mkdirs();
        if (file.exists()) file.delete();

        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        Set<ServicePair> servicePairs = getValidServicePair();
        ClassVisitor classVisitor = new ServiceManagerClassVisitor(classWriter, servicePairs);
        classReader.accept(classVisitor, EXPAND_FRAMES);
        byte[] code = classWriter.toByteArray();
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(code);
        } finally {
            fos.close();
        }
    }

    private Set<ServicePair> getValidServicePair() {
        Set<ServicePair> result = new HashSet<>();
        for (Map.Entry<String,String> entry : finalServicePairs.entrySet()) {
            String implName = entry.getKey();
            if (implName == null || implName.isEmpty()) continue;
            String interfaceName = entry.getValue();
            if (interfaceName == null || interfaceName.isEmpty()) continue;
            result.add(new ServicePair(implName, interfaceName));
        }
        return result;
    }

    /**
     * 处理文件目录下的class文件
     */
    File handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) throws IOException {
        File parent = directoryInput.getFile();
        if (parent.isDirectory()) {
            Utils.eachFileRecurse(parent, new FileHandler() {
                @Override
                public void handleFile(File file) throws IOException {
                    String name = file.getName();
                    if (!isServiceBySimpleClassName(name)) return;
                    byte[] bytes = Utils.getBytes(file);
                    checkDetailClassName(bytes);
                }
            });
        }
        //处理完输入文件之后，要把输出给下一个任务
        File dest = outputProvider.getContentLocation(directoryInput.getName(),
                directoryInput.getContentTypes(), directoryInput.getScopes(),
                Format.DIRECTORY);
        FileUtils.copyDirectory(directoryInput.getFile(), dest);
        return dest;
    }

    private void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider) throws IOException {
        boolean isJar = jarInput.getFile().getAbsolutePath().endsWith(JAR_POSTFIX);
        if (!isJar) return;

        String jarName = jarInput.getName();
        String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
        if (jarName.endsWith(JAR_POSTFIX)) jarName = jarName.substring(0, jarName.length() - 4);

        File tmpFile = new File(jarInput.getFile().getParent() + File.separator + TMP_JAR_FILE_NAME);
        if (tmpFile.exists()) tmpFile.delete();

        JarFile jarFile = new JarFile(jarInput.getFile());
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));

        try {
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                String simpleClassName = Utils.getSimpleClassNameForJarEntryName(entryName);

                boolean handled = false;

                //检查是否是ServiceImpl的class，如果是则记录下来
                boolean isServiceClassBySimpleName = isServiceBySimpleClassName(simpleClassName);
                if (isServiceClassBySimpleName) {
                    jarOutputStream.putNextEntry(zipEntry);
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    jarOutputStream.write(bytes);
                    checkDetailClassName(bytes);
                    handled = true;
                }

                //如果是ServiceManager，则记录其字节码之后踢出
                if (SERVICE_MANAGER_SIMPLE_CLASS_NAME.equals(simpleClassName)) {
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    ClassReader classReader = new ClassReader(bytes);
                    String rawClassName = classReader.getClassName();
                    String className = rawClassName.replace("/", ".");
                    if (SERVICE_MANAGER_FULL_NAME.equals(className)) {
                        settingManagerClassBytes = bytes;
                    } else {
                        jarOutputStream.putNextEntry(zipEntry);
                        jarOutputStream.write(bytes);
                    }
                    handled = true;
                }

                if (!handled) {
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }
        } finally {
            jarOutputStream.close();
            jarFile.close();
        }

        File dest = outputProvider.getContentLocation(jarName + md5Name,
                jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
        FileUtils.copyFile(tmpFile, dest);
        tmpFile.delete();
    }


    private boolean isServiceBySimpleClassName(String name) {
        return name.endsWith(".class") && implServiceSimpleClassNameSet.contains(name);
    }

    private void checkDetailClassName(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        //  "com/gavin/asmdemo/service/TwoService"
        String rawClassName = classReader.getClassName();

        String className = rawClassName.replace('/', '.');
        if (kaptServiceMap.containsKey(className)) {
            this.finalServicePairs.put(className, kaptServiceMap.get(className));
        }
    }
}