package com.example.plugin.service;

import com.android.build.api.transform.JarInput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static com.example.plugin.service.Constants.SERVICE_JSON_PATH;
import static com.example.plugin.service.Constants.TMP_JAR_FILE_NAME;


class Utils {

    public static String getSimpleClassNameForZipEntryName(String jarEntryName) {
        int nameDividerIndex = jarEntryName.lastIndexOf("/");
        if (nameDividerIndex < 0) return jarEntryName;
        return jarEntryName.substring(nameDividerIndex + 1);
    }

    public static Map<String, String> getAllServicePair(Project applicationProject) throws IOException {
        Map<String, String> result = new HashMap<>();

        Project rootProject = applicationProject.getRootProject();
        Set<Project> allProjects = rootProject.getAllprojects();

        for (Project project: allProjects) {
            File buildDir = project.getBuildDir();
            File jsonConfigFile = new File(buildDir, SERVICE_JSON_PATH);
            JSONObject json = Utils.getExistAnnotationInfo(jsonConfigFile);
            if (json == null) continue;

            Map<String,Object> configMap= json.toMap();
            for (Map.Entry<String, Object> entry: configMap.entrySet()) {
                Object value = entry.getValue();
                if (!(value instanceof String)) continue;

                String interfaceName = entry.getKey();
                String implName = (String) value;
                if (result.containsKey(interfaceName)) {
                    String serviceNameInfo = " with same service: " + interfaceName;
                    String serviceImplInfo = " serviceImplName: " + result.get(interfaceName) + " and " + implName;
                    throw new IllegalStateException("contains duplicated ServiceImpl !!!,  "  + serviceImplInfo + serviceNameInfo );
                }
                result.put(entry.getKey(), (String) value);
            }
        }
        return result;
    }

    @Nullable
    public static JSONObject getExistAnnotationInfo(File outputFile) throws IOException {
        if (outputFile == null) return null;

        if (!outputFile.exists()) return null;

        byte[] bytes = getBytes(outputFile);
        String fileContent = new String(bytes);

        return new JSONObject(fileContent);
    }

    /**
     * 从name中提取出className，
     * 如果这个name不是className，则返回null
     * */
    static String getClassName(String name) {
        int nameLength = name.length();
        if(nameLength <= 6) {
            return null;
        }
        String nameSuffix = name.substring(nameLength - 6 , nameLength);
        if(!".class".equals(nameSuffix)) {
            return null;
        }
        return name.substring(0 , nameLength - 6);
    }

    static byte[] copyJar(JarInput jarInput, File outputFile, @NotNull String exceptClassName) throws IOException {
        byte[] result = null;

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

                String className = Utils.getClassName(entryName);
                if (className == null) className = "";
                String realClassName = className.replace('/', '.');

                if (!exceptClassName.equals(realClassName)) {
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                } else {
                    result = IOUtils.toByteArray(inputStream);
                }
                jarOutputStream.closeEntry();
            }
        } finally {
            jarOutputStream.close();
            jarFile.close();
        }
        FileUtils.copyFile(tmpFile, outputFile);
        tmpFile.delete();

        return result;
    }

    public static Set<String> getImplSimpleClassNameSet(Map<String, String> kaptServiceMap) {
        Set<String> result = new HashSet<>();
        for (String implName : kaptServiceMap.values()) {
            String simpleClassName = getSimpleClassName(implName);
            if (simpleClassName != null) result.add(simpleClassName);
        }
        return result;
    }

    private static String getSimpleClassName(String fullClassName) {
        if (fullClassName == null) return null;
        if (fullClassName.isEmpty()) return null;

        int dotIndex = fullClassName.lastIndexOf(".");
        String className;
        if (dotIndex < 0) {
            className = fullClassName;
        } else {
            className = fullClassName.substring(dotIndex + 1);
        }
        return className + ".class";
    }

    private static void checkDir(File dir) throws FileNotFoundException, IllegalArgumentException {
        if (!dir.exists()) {
            throw new FileNotFoundException(dir.getAbsolutePath());
        } else if (!dir.isDirectory()) {
            throw new IllegalArgumentException("The provided File object is not a directory: " + dir.getAbsolutePath());
        }
    }

    public static void eachFileRecurse(File self,  FileHandler closure) throws IOException, IllegalArgumentException {
        checkDir(self);
        File[] files = self.listFiles();
        if (files != null) {
            File[] var4 = files;
            int var5 = files.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                File file = var4[var6];
                if (file.isDirectory()) {
                    eachFileRecurse(file, closure);
                } else  {
                    closure.handleFile(file);
                }
            }
        }
    }

    public static byte[] getBytes(File file) throws IOException {
        return getBytes(new FileInputStream(file));
    }

    public static byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream answer = new ByteArrayOutputStream();
        byte[] byteBuffer = new byte[8192];

        int nbByteRead;
        try {
            while((nbByteRead = is.read(byteBuffer)) != -1) {
                answer.write(byteBuffer, 0, nbByteRead);
            }
        } finally {
            is.close();
        }

        return answer.toByteArray();
    }

}
