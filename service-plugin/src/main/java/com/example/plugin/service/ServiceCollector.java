package com.example.plugin.service;

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

    public Map<String, String> getAllServicePair() {
        Map<String, String> result = new HashMap<>();

        String interfaceName = "com.gavin.asmdemo.service.TwoService";
        String implName = "com.gavin.asmdemo.service.TwoServiceImpl";
        result.put(implName, interfaceName);

        interfaceName = "com.gavin.asmdemo.service.ThirdService";
        implName = "com.gavin.asmdemo.service.ThirdServiceImpl";
        result.put(implName, interfaceName);

        // TODO: 2020/9/14 检查Impl是否有重复，如果有重复，使编译过程直接crash

        return result;
    }
}
