package com.gavin.asmdemo.service;

import com.example.service_anno.ServiceImpl;

@ServiceImpl(service = { TwoService.class })
public class TwoServiceImpl implements TwoService {
    @Override
    public int doBusinessOne() {
        return 0;
    }

    @Override
    public String doBusinessTwo() {
        return "doBusinessTwo";
    }
}
