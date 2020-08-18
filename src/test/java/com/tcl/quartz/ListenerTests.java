package com.tcl.quartz;

import com.tcl.quartz.job.HelloJob;
import com.tcl.quartz.listener.MyJobListener;
import com.tcl.quartz.listener.MyTriggerListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;

import java.util.concurrent.TimeUnit;

import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/18 19:33
 */
public class ListenerTests {

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
    void jobListenerTest() throws Exception {
        Trigger trigger = newTrigger().startNow().build();
        scheduler.getListenerManager().addJobListener(new MyJobListener());
        scheduler.scheduleJob(job, trigger);
        TimeUnit.SECONDS.sleep(2);
    }


    @Test
    void triggerListenerTest() throws Exception{
        Trigger trigger = newTrigger().startNow().withIdentity("trigger", "group").build();
        scheduler.getListenerManager()
                .addTriggerListener(new MyTriggerListener(),
                        KeyMatcher.keyEquals(new TriggerKey("trigger", "group")));
        scheduler.scheduleJob(job, trigger);
        TimeUnit.SECONDS.sleep(1);
    }
}
