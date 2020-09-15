package com.gavin.plugin.lifecycle;


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
