package com.example.plugin.service;


class SettingPair {

    //"com.gavin.asmdemo.service.ThirdService"
    String interfaceName;

    //"com.gavin.asmdemo.service.ThirdServiceImpl"
    String implName;


    @Override
    public String toString() {
        return "SettingPair [interfaceName=" + interfaceName + ", implName=" + implName + "]";
    }
}
