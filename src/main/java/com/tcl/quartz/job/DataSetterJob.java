package com.tcl.quartz.job;

import lombok.Setter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/18 9:29
 */
@Setter
public class DataSetterJob implements Job {
    private String name;
    private int state;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey key = context.getJobDetail().getKey();
        System.err.println(key + "{name: " + name + "; state: " + state + "}");
    }
}
