package com.tcl.quartz.service;

import org.springframework.stereotype.Service;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/19 13:38
 */
@Service
public class SimpleJobService {

    public void doSimpleJob() {
        System.err.println("Executing Simple Job");
    }
}
