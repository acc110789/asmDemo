package com.gavin.plugin.lifecycle

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class SettingManagerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        //registerTransform
        def android = project.extensions.getByType(AppExtension)
        SettingManagerTransform transform = new SettingManagerTransform(project)
        android.registerTransform(transform)
    }

    static void handleFileRecurse(File parent, FileHandler handler) {
        parent.eachFileRecurse { file ->
            handler.handleFile(file)
        }
    }

    static byte[] getFileBytes(File file) {
        return file.bytes
    }
}