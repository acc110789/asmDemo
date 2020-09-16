package com.example.plugin.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


class Utils {

    public static String getSimpleClassNameForJarEntryName(String jarEntryName) {
        int nameDividerIndex = jarEntryName.lastIndexOf("/");
        if (nameDividerIndex < 0) return jarEntryName;
        return jarEntryName.substring(nameDividerIndex + 1);
    }

    public static Set<String> getImplSimpleClassNameSet(Map<String, String> kaptServiceMap) {
        Set<String> result = new HashSet<>();
        for (String implName : kaptServiceMap.keySet()) {
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
