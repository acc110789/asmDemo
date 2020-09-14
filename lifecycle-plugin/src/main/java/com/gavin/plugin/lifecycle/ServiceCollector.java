package com.gavin.plugin.lifecycle;

import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ServiceCollector {
    private final Project applicationProject;

    public ServiceCollector(Project project) {
        this.applicationProject = project;
    }

    public Map<String, SettingPair> collectSettingPair() {
        Map<String, SettingPair> result = new HashMap<>();

        String twoKey = "two_service";
        SettingPair twoPair = new SettingPair();
        twoPair.interfaceName = "com.gavin.asmdemo.service.TwoService";
        twoPair.implName = "com.gavin.asmdemo.service.TwoServiceImpl";
        result.put(twoKey, twoPair);

        String thirdKey = "third_service";
        SettingPair thirdPair = new SettingPair();
        thirdPair.interfaceName = "com.gavin.asmdemo.service.ThirdService";
        thirdPair.implName = "com.gavin.asmdemo.service.ThirdServiceImpl";
        result.put(thirdKey, thirdPair);

        // TODO: 2020/9/14 检查Impl是否有重复，如果有重复，使编译过程直接crash

        return result;
    }

    /**
     * class list
     * */
    public Set<String> collect() {
        Set<String> result = new HashSet<>();

        Set<Project> projectSet = applicationProject.getRootProject().getAllprojects();
        for (Project project : projectSet) {
            List<String> serviceInfo = getServiceInfoOrNull(project);
            if (serviceInfo == null) continue;
            result.addAll(serviceInfo);
        }

        return result;
    }

    private List<String> getServiceInfoOrNull(Project project) {
        if (project == null) return null;

        Logger.log("collect info from project: " + project.getName());

        File projectDir = project.getProjectDir();
        File serviceJsonFile = new File(projectDir, "service.json");
        if (!serviceJsonFile.exists()) return null;
        Logger.log("collect info from file: " + serviceJsonFile.getAbsolutePath());

        List<String> result = new ArrayList<>();
        result.add("ThirdServiceImpl.class");
        result.add("TwoServiceImpl.class");
        result.add("TwoService.class");
        result.add("ThirdService.class");
        return result;
    }

}
