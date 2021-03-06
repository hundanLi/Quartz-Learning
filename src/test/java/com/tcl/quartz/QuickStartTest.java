package com.tcl.quartz;

import com.tcl.quartz.job.HelloJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/15 16:58
 */
public class QuickStartTest {

    public static void main(String[] args) {
        try {
            // 1.构建Scheduler实例
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            System.out.println("Quartz Simple Test Started");

            // 2.定义任务
            JobDetail job = JobBuilder.newJob(HelloJob.class)
                    .withIdentity("job1", "group1")
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

}
