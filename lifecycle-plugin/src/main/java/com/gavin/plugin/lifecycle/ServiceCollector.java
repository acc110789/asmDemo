package com.gavin.plugin.lifecycle;

import org.gradle.api.Project;

import java.util.HashSet;
import java.util.Set;

class ServiceCollector {
    private final Project applicationProject;
    private final Set<Project> projectSet = new HashSet<>();

    public ServiceCollector(Project project) {
        this.applicationProject = project;
    }

    public void collect() {
        collectAllProject();
    }

    private void collectAllProject() {
        projectSet.clear();
        applicationProject.getDependencies();
        applicationProject.getAllprojects();
        applicationProject.getDependencies();
        applicationProject.getSubprojects();
    }

}
