package com.tcl.quartz;

import com.tcl.quartz.job.HelloJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.DateBuilder.*;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/18 13:50
 */
public class TriggerTests {

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
    void onceInTimeTest() throws SchedulerException, InterruptedException {
        Date date = new Date();
        date.setTime(new Date().getTime() + 5000);
        Trigger trigger = newTrigger().withIdentity("trigger1", "group")
                .startAt(date)
                .build();

        scheduler.scheduleJob(job, trigger);
        Thread.sleep(10000);

    }

    @Test
    void inTimeWithRepeat() throws SchedulerException, InterruptedException {
        SimpleTrigger trigger = newTrigger()
                .withIdentity("trigger2", "group")
                .startNow()
                .withSchedule(
                        simpleSchedule()
                                .withIntervalInSeconds(10)
                                .withRepeatCount(10)
                )
                .build();

        scheduler.scheduleJob(job, trigger);

        Thread.sleep(110000);
    }

    @Test
    void inTime5minLater() throws SchedulerException, InterruptedException {
        Trigger trigger = newTrigger()
                .withIdentity("trigger3", "group")
                .startAt(futureDate(2, IntervalUnit.MINUTE))
                .build();
        scheduler.scheduleJob(job, trigger);

        Thread.sleep(130000);
    }


    @Test
    void interval5minUtil() throws SchedulerException, InterruptedException {
        SimpleTrigger trigger = newTrigger()
                .withIdentity("trigger4", "group")
                .startNow()
                .withSchedule(
                        simpleSchedule()
                                .repeatForever()
                                .withIntervalInMinutes(5)
                )
                .endAt(dateOf(15, 0, 0))
                .build();

        scheduler.scheduleJob(job, trigger);
        TimeUnit.MINUTES.sleep(25);

    }
}
