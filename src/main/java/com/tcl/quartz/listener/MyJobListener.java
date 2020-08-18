package com.tcl.quartz.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.listeners.JobListenerSupport;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/18 19:25
 */
public class MyJobListener extends JobListenerSupport {
    @Override
    public String getName() {
        return "MyJobListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        JobKey key = context.getJobDetail().getKey();
        System.err.println("Job{"+key+"} is about to be executed..." );
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        JobKey key = context.getJobDetail().getKey();
        System.err.println("Job{"+key+"} has been executed successfully..." );
    }
}
