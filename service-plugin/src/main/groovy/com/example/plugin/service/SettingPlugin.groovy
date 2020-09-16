package com.example.plugin.service

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class SettingPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        //registerTransform
        def android = project.extensions.getByType(AppExtension)
        ServiceTransform transform = new ServiceTransform(project)
        android.registerTransform(transform)
    }
}