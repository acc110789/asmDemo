package com.gavin.plugin.lifecycle;

import com.android.utils.FileUtils;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


class Utils {

    private static final int BUFFER = 8192;
    private static final String TMP_FILE_NAME = "_msg_item_view_merge_mapping_tmp_utils";

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

    public static Set<String> getSimpleClassNameSet(Map<String, SettingPair> configSettingPair) {
        Set<String> result = new HashSet<>();

        for (SettingPair pair : configSettingPair.values()) {
            String simpleClassName = getSimpleClassName(pair.interfaceName);
            if (simpleClassName != null) result.add(simpleClassName);

            simpleClassName = getSimpleClassName(pair.implName);
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

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    static void copyJar(File inputFile, File outputFile, @NotNull String exceptClassName) {
        String outFilePath = outputFile.getAbsolutePath();
        String tmpFileName = outputFile.getParentFile().getAbsolutePath() + TMP_FILE_NAME;
        File tmpFile = new File(tmpFileName);
        if(tmpFile.exists()) {
            deleteDir(tmpFile);
        }
        tmpFile.mkdirs();

        ClassPool cp = ClassPool.getDefault();
        String inputFilePath = inputFile.getAbsolutePath();

        try {
            //load Zip
            cp.insertClassPath(inputFilePath);
            ZipInputStream zis;
            zis = new ZipInputStream(new FileInputStream(inputFilePath));
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                String className = getClassName(name);
                if(className == null) {
                    continue;
                }
                String realClassName = className.replace('/', '.');

                if(!exceptClassName.equals(realClassName)) {
                    CtClass ctClass = cp.get(realClassName);
                    ctClass.writeFile(tmpFileName);
                }
            }
            //写完毕之后，重新压缩tmpFile，输出到outputFile里面
            compress(tmpFileName , outFilePath);
            //最后删除临时的目录
            deleteDir(tmpFile);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }


    private static void compress(String srcPath, String dstPath) throws IOException {
        File srcFile = new File(srcPath);
        File dstFile = new File(dstPath);
        if (!srcFile.exists()) {
            throw new FileNotFoundException(srcPath + "不存在！");
        }

        FileOutputStream out = null;
        ZipOutputStream zipOut = null;
        try {
            out = new FileOutputStream(dstFile);
            CheckedOutputStream cos = new CheckedOutputStream(out, new CRC32());
            zipOut = new ZipOutputStream(cos);
            String baseDir = "";
            compress(srcFile, zipOut, baseDir);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        } finally {
            if (null != zipOut) {
                zipOut.close();
                out = null;
            }
            if (null != out) {
                out.close();
            }
        }
    }

    private static void compress(File file, ZipOutputStream zipOut, String baseDir) throws IOException {
        if (file.isDirectory()) {
            compressDir(file, zipOut, baseDir);
        } else {
            compressFile(file, zipOut, baseDir);
        }
    }

    /**
     * 压缩一个目录
     */
    private static void compressDir(File dir, ZipOutputStream zipOut, String baseDir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            compress(file, zipOut, baseDir + dir.getName() + "/");
        }
    }

    /**
     * 压缩一个文件
     */
    private static void compressFile(File file, ZipOutputStream zipOut, String baseDir) {
        if (!file.exists()) {
            return;
        }
        baseDir = baseDir.substring(baseDir.indexOf("/") + 1, baseDir.length());
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            ZipEntry entry = new ZipEntry(baseDir + file.getName());
            zipOut.putNextEntry(entry);
            int count;
            byte data[] = new byte[BUFFER];
            while ((count = bis.read(data, 0, BUFFER)) != -1) {
                zipOut.write(data, 0, count);
            }
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        } finally {
            if (null != bis) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void copyFileSilent(File input, File output) {
        try {
            FileUtils.copyFile(input , output);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void tryCreateAndCopyFileSilent(File input, File output) {
        //先创建这个文件
        output.getParentFile().mkdirs();
        if(!output.exists()) {
            try {
                output.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        copyFileSilent(input , output);
    }

    static void copyDirectorySilent(File input, File output) {
        try {
            FileUtils.copyDirectory(input , output);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
