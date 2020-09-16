package com.example.service_compiler;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

class Utils {

    public static void writeJSONObjectToFile(JSONObject json, File outputFile) throws IOException {
        if (outputFile == null) return;
        String result = json.toString();
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        try {
            outputStream.write(result.getBytes());
            outputStream.flush();
        } finally {
            outputStream.close();
        }
    }

    public static String removeClassPostfix(String raw) {
        if (raw == null) return null;
        if (raw.endsWith(".class")) {
            return raw.substring(0, raw.length() - 6);
        }
        return raw;
    }

    @Nullable
    public static JSONObject getExistAnnotationInfo(File outputFile) throws IOException {
        if (outputFile == null) return null;

        if (!outputFile.exists()) return null;

        byte[] bytes = getBytes(outputFile);
        String fileContent = new String(bytes);

        return new JSONObject(fileContent);
    }

    private static byte[] getBytes(File file) throws IOException {
        return getBytes(new FileInputStream(file));
    }

    private static byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream answer = new ByteArrayOutputStream();
        byte[] byteBuffer = new byte[8192];
        int byteCount;
        try {
            while((byteCount = is.read(byteBuffer)) != -1) {
                answer.write(byteBuffer, 0, byteCount);
            }
        } finally {
            is.close();
        }
        return answer.toByteArray();
    }


    public static JSONObject mergeJson(JSONObject one, JSONObject two) {
        JSONObject result = new JSONObject();

        Map<String, Object> map = one.toMap();
        for (Map.Entry<String, Object> entry: map.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        map = two.toMap();
        for (Map.Entry<String, Object> entry: map.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

}