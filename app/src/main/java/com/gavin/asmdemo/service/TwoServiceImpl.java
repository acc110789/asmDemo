package com.gavin.asmdemo.service;

import com.example.service_anno.ServiceImpl;

@ServiceImpl(name = ServiceNames.TWO_SERVICE)
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
