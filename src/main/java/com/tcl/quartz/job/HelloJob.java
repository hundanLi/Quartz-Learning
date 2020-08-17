package com.tcl.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.LocalDateTime;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/15 17:07
 */
public class HelloJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println(LocalDateTime.now() + " HelloJob Running:" + jobExecutionContext.getJobInstance());
    }
}
