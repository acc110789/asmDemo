package com.gavin.asmdemo.service;

import com.example.service_anno.ServiceImpl;

@ServiceImpl(service = { ThirdService.class })
public class ThirdServiceImpl implements ThirdService {
    @Override
    public int doBusiness1() {
        return 0;
    }

    @Override
    public String doBusiness2() {
        return null;
    }
}
