package com.tcl.quartz.job;

import org.quartz.*;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/18 9:45
 */
public class DataMergeJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey key = context.getJobDetail().getKey();
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String name = jobDataMap.getString("name");
        String foo = jobDataMap.getString("foo");
        int state = jobDataMap.getInt("state");

        System.err.println(key + " JobDataMap: { name=" + name + "; foo=" + foo + "; state=" + state + " }");
    }
}
