package com.tcl.quartz.config;

import com.tcl.quartz.job.SimpleJob;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;

import java.io.IOException;
import java.util.Properties;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/19 13:39
 */
//@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail jobDetail() {
        return JobBuilder.newJob().ofType(SimpleJob.class)
                .storeDurably(true)
                .withIdentity("simple-job", "simple-group")
                .withDescription("simple job")
                .build();
    }

    @Bean
    public Trigger trigger() {
        return TriggerBuilder
                .newTrigger()
                .withIdentity("simple-job", "simple-group")
                .withDescription("simple description")
                .withSchedule(
                        simpleSchedule()
                                .withIntervalInMinutes(1)
                                .withRepeatCount(10)
                )
                .build();

    }


    @Bean
    @QuartzDataSource
    public DataSource quartzDataSource() throws IOException {
        ClassPathResource resource = new ClassPathResource("hikaricp.properties");
        Properties properties = new Properties();
        properties.load(resource.getInputStream());
        HikariConfig hikariConfig = new HikariConfig(properties);
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfigLocation(new ClassPathResource("quartz.properties"));
        factoryBean.setSchedulerName("springScheduler");
        return factoryBean;
    }


    @Bean
    public Scheduler springScheduler(SchedulerFactoryBean schedulerFactoryBean) {
        return schedulerFactoryBean.getScheduler();
    }


    @Bean
    public Scheduler stdScheduler(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.deleteJob(jobDetail.getKey());
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
        return scheduler;
    }


}
