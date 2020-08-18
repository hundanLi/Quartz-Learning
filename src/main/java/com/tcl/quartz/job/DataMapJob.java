package com.tcl.quartz.job;

import org.quartz.*;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/17 20:48
 */
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class DataMapJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        // JobDetail的唯一标识：group+name
        JobKey jobKey = jobDetail.getKey();

        JobDataMap dataMap = jobDetail.getJobDataMap();
        int number = dataMap.getInt("number");
        dataMap.put("number", number+1);
        String string = dataMap.getString("string");
        System.err.println("JobDetail#"+"{ " + jobKey + "}: " + "number=" + number + "; string=" + string);

    }
}
