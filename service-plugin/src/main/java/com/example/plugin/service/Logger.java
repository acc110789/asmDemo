package com.example.plugin.service;

class Logger {

    public static void log(String msg) {
        if (msg == null || msg.isEmpty()) return;
        System.out.println("SettingManagerTransform: " + msg);
    }
}
