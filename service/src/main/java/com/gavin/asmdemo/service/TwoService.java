package com.gavin.asmdemo.service;

import com.example.service_anno.Service;


@Service(name = ServiceNames.TWO_SERVICE)
public interface TwoService {
    int doBusinessOne();

    String doBusinessTwo();
}
