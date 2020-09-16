package com.example.plugin.service;


import org.jetbrains.annotations.NotNull;

class ServicePair {

    //"com.gavin.asmdemo.service.ThirdService"
    @NotNull
    final String interfaceName;

    //"com.gavin.asmdemo.service.ThirdServiceImpl"
    @NotNull
    final String implName;

    public ServicePair(@NotNull String implName,@NotNull String interfaceName) {
        this.implName = implName;
        this.interfaceName = interfaceName;
    }

    @Override
    public String toString() {
        return "SettingPair [interfaceName=" + interfaceName + ", implName=" + implName + "]";
    }
}
