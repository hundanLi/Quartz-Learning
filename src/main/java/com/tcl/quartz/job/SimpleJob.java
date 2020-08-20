package com.tcl.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/19 13:40
 */
public class SimpleJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        System.err.println("Simple Job executing...");
    }
}
