package com.gavin.asmdemo.service;

import com.example.service_anno.Service;

@Service(name = ServiceNames.THIRD_SERVICE)
public interface ThirdService {
    int doBusiness1();

    String doBusiness2();
}
