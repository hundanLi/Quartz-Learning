package com.tcl.quartz;

import com.tcl.quartz.job.HelloJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.quartz.DateBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/18 13:50
 */
public class CronTriggerTests {

    Scheduler scheduler;
    JobDetail job;

    @BeforeEach
    void beforeEach() throws SchedulerException {

        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        job = JobBuilder.newJob(HelloJob.class)
                .withIdentity("job1", "group1")
                .build();
    }

    @AfterEach
    void afterEach() throws SchedulerException {
        scheduler.shutdown();
    }

    @Test
    void everyEvenSecondTest() throws SchedulerException, InterruptedException {
        Trigger trigger = newTrigger().withIdentity("cron-trigger1", "cron-group")
                .withSchedule(
                    cronSchedule("0 0/2 9-18 * * ?")
                )
                .build();

        scheduler.scheduleJob(job, trigger);
        TimeUnit.HOURS.sleep(1);

    }

    @Test
    void DailyInTimeTest() throws SchedulerException, InterruptedException {
        Trigger trigger = newTrigger().withIdentity("trigger2", "group")
                .withSchedule(
                        cronSchedule("0 30 16 * * ?")
                )
                .build();

        scheduler.scheduleJob(job, trigger);
        TimeUnit.MINUTES.sleep(10);
    }

    @Test
    void SomeWeekdayInTimeTest() throws SchedulerException, InterruptedException {
        Trigger trigger = newTrigger().withIdentity("trigger2", "group")
                .withSchedule(
                        cronSchedule("0 30 16 ? * TUE")
                        .inTimeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                )
                .build();

        scheduler.scheduleJob(job, trigger);
        TimeUnit.MINUTES.sleep(10);
    }
}
