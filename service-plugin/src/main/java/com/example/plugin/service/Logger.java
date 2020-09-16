package com.example.plugin.service;

import static com.example.plugin.service.Constants.LOG_TAG;

class Logger {
    public static void log(String msg) {
        if (msg == null || msg.isEmpty()) return;
        System.out.println(LOG_TAG + ": " + msg);
    }
}
