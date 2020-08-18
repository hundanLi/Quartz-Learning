package com.tcl.quartz;

import com.tcl.quartz.job.DataMapJob;
import com.tcl.quartz.job.DataMergeJob;
import com.tcl.quartz.job.DataSetterJob;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/18 13:48
 */
public class JobTests {
    @Test
    void jobDataMapTest() {
        try {
            // 1.构建Scheduler实例
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            System.out.println("Quartz JobDataMap Test");

            // 2.定义任务
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("number", 10);
            dataMap.put("string", "str");
            JobDetail job = JobBuilder.newJob(DataMapJob.class)
                    .withIdentity("job1", "group1")
                    .usingJobData(dataMap)
                    .build();

            // 3.定义触发器
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .startNow()
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(10)
                                    .repeatForever()
                    ).build();
            // 4.开启任务调度
            scheduler.scheduleJob(job, trigger);

            // 5.main线程睡眠60s
            Thread.sleep(60000);

            scheduler.shutdown();
        } catch (SchedulerException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void jobSetterTest() {
        try {
            // 1.构建Scheduler实例
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            System.out.println("Quartz Job Setter Test");

            // 2.定义任务
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("state", 10);
            dataMap.put("name", "name");
            JobDetail job = JobBuilder.newJob(DataSetterJob.class)
                    .withIdentity("job1", "group1")
                    .usingJobData(dataMap)
                    .build();

            // 3.定义触发器
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .startNow()
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(10)
                                    .repeatForever()
                    ).build();
            // 4.开启任务调度
            scheduler.scheduleJob(job, trigger);

            // 5.main线程睡眠60s
            Thread.sleep(60000);

            scheduler.shutdown();
        } catch (SchedulerException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void jobDataMergeTest() {
        try {
            // 1.构建Scheduler实例
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            System.out.println("Quartz Job DataMerge Test");

            // 2.定义任务
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("state", 10);
            dataMap.put("name", "DetailName");
            JobDetail job = JobBuilder.newJob(DataMergeJob.class)
                    .withIdentity("job1", "group1")
                    .usingJobData(dataMap)
                    .build();

            // 3.定义触发器
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .usingJobData("name", "TriggerName")
                    .usingJobData("foo", "bar")
                    .startNow()
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(10)
                                    .repeatForever()
                    ).build();
            // 4.开启任务调度
            scheduler.scheduleJob(job, trigger);

            // 5.main线程睡眠60s
            Thread.sleep(60000);

            scheduler.shutdown();
        } catch (SchedulerException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
