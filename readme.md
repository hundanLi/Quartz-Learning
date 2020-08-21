# Quartz学习笔记

[参考文档](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/)

[完整代码](https://github.com/hundanLi/Quartz-Learning)

## 1. 快速入门

### 1.1 简介

Quartz是一个开源的任务调度器。常见的应用场景：

- 定时任务
- 周期性服务

### 1.2 导入依赖

创建maven项目，并添加以下依赖

```xml

        <dependency>
            <groupId>org.quartz-scheduler</groupId>
            <artifactId>quartz</artifactId>
            <version>2.3.2<version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.3</version>
        </dependency>
```



### 1.3 简单配置

在`src/main/resources`目录下新建`quartz.properties`文件：

```properties
org.quartz.scheduler.instanceName = MyScheduler # 调度器名称
org.quartz.threadPool.threadCount = 3	# 线程池大小
org.quartz.jobStore.class = org.quartz.simpl.RAMJobStore # 存储类型
```

这里分别配置了调度器的实例名称，线程池的线程数，任务存储类型（in-memery）

### 1.4 代码测试

1. 实现`Job`接口定义任务

   ```java
   public class HelloJob implements Job {
       @Override
       public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
           System.out.println(LocalDateTime.now() + " HelloJob Running:" + jobExecutionContext.getJobInstance());
       }
   }
   ```

   实现了`Job`接口就可以作为任务进行调度。

   

2. 编写测试

   ```java
   public class QuartzTest {
   
       public static void main(String[] args) {
           try {
               // 1.构建Scheduler实例并启动
               Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
               scheduler.start();
               System.out.println("Quartz Simple Test Started......");
   
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
   			
               // 6.调度器关闭
               scheduler.shutdown();
           } catch (SchedulerException | InterruptedException e) {
               e.printStackTrace();
           }
       }
   }
   ```



## 2. 功能特性

### 2.1 核心概念

1. 调度器Scheduler

   调度器的生命周期从通过**SchedulerFactory** 创建开始，到调用*shutdown()*方法结束。实例创建完成便可以添加或者移除Job和Trigger，只有*start()*方法被调用，Scheduler才真正开始进行任务调度和执行工作。

2. 作业 Job和 JobDetail

   Job是一个定义任务的接口，只含有一个*execute*方法，只有实现该接口的组件才能被Scheduler执行。当与之绑定的触发器被触发时，*execute*方法就会被scheduler的工作线程执行。*JobExecutionContext* 类型的参数被传递到方法中，通过该参数可以获得Job实例，Scheduler，Trigger，JobDetail等对象的引用。

   JobDetail 用来设置 Job 实例的各种属性和状态参数。

3. 触发器Trigger

   触发器定义了触发任务的条件，常用的触发器有 SimpleTrigger 和 CronTrigger。

4. Identities标识

   Job 和 Trigger 在注册到调度器时通过 *group* 和 *name* 来进行标识，同一个*group*内的名称必须唯一。



### 2.2 Job/JobDetail

`JobDetail`封装了`Job`任务，并为`Job`实例提供各种属性。可以通过`org.quartz.JobBuilder`类构建`JobDetail`实例。

```java
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
```

#### 1.`Job`实例的生命周期

调度器在每次执行`execute(...)`方法之前，都会先通过`JobFactory`创建对应的`Job`类的实例，执行完毕后将会删除对这个实例的引用，以便进行垃圾回收。这无形中造成两个效应：

- 使用 `JobFactory`接口默认的实现类`SimpleJobFactory`时，`Job`的实现类必须含有无参构造器

  每次执行任务都会调用`SimpleJobFactory#newJob`方法创建实例：

  ```java
  public Job newJob(TriggerFiredBundle bundle, Scheduler Scheduler) throws SchedulerException {
  
      JobDetail jobDetail = bundle.getJobDetail();
      Class<? extends Job> jobClass = jobDetail.getJobClass();
      try {
          // 调用默认无参构造方法
          return jobClass.newInstance();
      } catch (Exception e) {
          ...
      }
  }
  ```

  

- `Job`的实现类的状态字段毫无意义（与`JobDataMap`的键名称一致的除外），因为每次执行任务都会新建`Job`实例。

#### 2.`Job`与`JobDetail`的区别

`Job`是一个作业类，它包含任务执行逻辑，`JobDetail`是一个作业定义类，将会被注册到调度器上，可以创建多个绑定相同`Job`类的实例，并使用`JobDataMap`传入不同的参数，在任务执行时产生不同的效果。



#### 3.并发注解

`Job`类中可以添加几个注解，这些注解会影响Quartz在相应方面的行为。

- `@DisallowConcurrentExecution`：告知调度器不要并行地执行相同作业定义即`JobDetail`类的多个实例。

- `@PersistJobDataAfterExecution`：告知调度器在执行完`execute`方法后更新与`JobDetail`实例绑定的`JobDataMap`的属性，以便下次执行时访问到更新后的属性。

通常这两个注解同时使用，防止出现并发问题。

**思考**：为什么不加`@PersistJobDataAfterExecution`注解就不会更新`JobDataMap`的数据？

1. 调度线程`QuartzSchedulerThread`在触发Trigger时首先会从`JobStore`（如RAM，JDBC）中获取任务（通过`JobStore#triggersFired()`方法），然后再创建`JobRunShell`来执行任务。

   ```java
   // QuartzSchedulerThread#run()方法片段：
   boolean goAhead = true;
   synchronized(sigLock) {
       goAhead = !halted.get();
   }
   if(goAhead) {
       // 激活JobStore中的触发器
       try {
           // triggersFired方法获取触发成功的作业定义实例
           List<TriggerFiredResult> res = qsRsrcs.getJobStore().triggersFired(triggers);
           if(res != null)
               bndles = res;
       } catch (SchedulerException se) {
           qs.notifySchedulerListenersError(
               "An error occurred while firing triggers '"
               + triggers + "'", se);
           //QTZ-179 : a problem occurred interacting with the triggers from the db
           //we release them and loop again
           for (int i = 0; i < triggers.size(); i++) {
               qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
           }
           continue;
       }
   
   }
   
   for (int i = 0; i < bndles.size(); i++) {
       // 遍历触发成功的作业实例创建运行环境
       TriggerFiredResult result =  bndles.get(i);
       TriggerFiredBundle bndle =  result.getTriggerFiredBundle();
       Exception exception = result.getException();
   
       if (exception instanceof RuntimeException) {
           getLog().error("RuntimeException while firing trigger " + triggers.get(i), exception);
           qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
           continue;
       }
   
       // it's possible to get 'null' if the triggers was paused,
       // blocked, or other similar occurrences that prevent it being
       // fired at this time...  or if the scheduler was shutdown (halted)
       if (bndle == null) {
           qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
           continue;
       }
   	// 创建JobRunShell任务执行环境
       JobRunShell shell = null;
       try {
           shell = qsRsrcs.getJobRunShellFactory().createJobRunShell(bndle);
           shell.initialize(qs);
       } catch (SchedulerException se) {
           qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
           continue;
       }
   
       if (qsRsrcs.getThreadPool().runInThread(shell) == false) {
           // this case should never happen, as it is indicative of the
           // scheduler being shutdown or a bug in the thread pool or
           // a thread pool being used concurrently - which the docs
           // say not to do...
           getLog().error("ThreadPool.runInThread() return false!");
           qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
       }
   
   }
   ```

   

2. `JobRunShell`在调用`run()`方法执行任务，执行完成后，将调用`qs.notifyJobStoreJobComplete(trigger, jobDetail, instCode);`来通知调度器更新状态：

   ```java
   // 269行
   qs.notifyJobStoreJobComplete(trigger, jobDetail, instCode);
   ```

   

3. 调度器又会调用`JobStore#notifyJobStoreJobComplete()`方法来将`JobDataMap`的修改写入`JobStore`中，下次

   ```java
   
   public void triggeredJobComplete(OperableTrigger trigger,
                                    JobDetail jobDetail, CompletedExecutionInstruction triggerInstCode) {
   
       synchronized (lock) {
   		// 获取存储中的JobDetail
           JobWrapper jw = jobsByKey.get(jobDetail.getKey());
           TriggerWrapper tw = triggersByKey.get(trigger.getKey());
   
           // It's possible that the job is null if:
           //   1- it was deleted during execution
           //   2- RAMJobStore is being used only for volatile jobs / triggers
           //      from the JDBC job store
           if (jw != null) {
               // 存储中的JobDetail
               JobDetail jd = jw.jobDetail;
   			// 检查是否标注了@PersistJobDataAfterExecution注解
               if (jd.isPersistJobDataAfterExecution()) {
                   // 克隆新数据
                   JobDataMap newData = jobDetail.getJobDataMap();
                   if (newData != null) {
                       newData = (JobDataMap)newData.clone();
                       newData.clearDirtyFlag();
                   }
                   // 更新存储
                   jd = jd.getJobBuilder().setJobData(newData).build();
                   jw.jobDetail = jd;
               }
           ......
       }
   }
   ```

   



### 2.3 JobDataMap

如前所述，`Job`实例无法维护状态，它的配置/属性依赖于`JobDataMap`。JobDataMap是Java Map接口的实现，并且具有一些用于存储和检索基本类型数据的便捷方法，可以用它来保存`Job`实例的状态和数据。示例：

#### 1.显示设置和获取JobDataMap

`Job`实现类

```java
public class DataMapJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        // JobDetail的唯一标识：group+name
        JobKey jobKey = jobDetail.getKey();

        JobDataMap dataMap = jobDetail.getJobDataMap();
        int number = dataMap.getInt("number");
        String string = dataMap.getString("string");
        System.err.println("JobDetail{ " + jobKey + "}: " + "number=" + number + "; string=" + string);

    }
}
```

任务定义和调度：

```java
void jsonDataMapTest() {
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
            .usingJobData(dataMap)	// 绑定数据
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
```

需要注意的是尽量在JobDataMap中存储JDK内置类型的数据，消除出现序列化问题的可能。



#### 2.使用同名的`Job`属性和Map键

如果在`Job`类中添加与`JobDataMap`中的键名称相对应的setter方法，则Quartz的默认JobFactory实现将在实例化`Job`时自动调用这些setter，从而避免了需要在execute方法中从映射中显式获取值。示例：

`Job`类：

```java
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
```

测试：

```java
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
```



#### 3. Trigger关联的JobDataMap

除了`Job`，触发器也可以有与之关联的`JobDataMap`。如果有一个作业存储在调度程序中以供多个触发器正常/重复使用，但是对于每个独立的触发器，你都希望为该作业提供不同的数据输入，这将很有用。

在任务`execute()`方法执行过程中，可以调用`JobExecutionContext#getMergedJobDataMap()`方法获取到`JobDetail`和`Trigger`合并后的数据，后者将覆盖前者同名的属性。示例：

`Job`类：

```java
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
```

测试：

```java
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
```



### 2.4 Triggers

#### 2.4.1 通用属性

| 属性名               | 描述                                                     |
| -------------------- | -------------------------------------------------------- |
| jobKey               | 作业的唯一标识                                           |
| startTime            | 触发器生效的时间                                         |
| endTime              | 触发器失效的时间                                         |
| priority             | 触发执行优先级                                           |
| misfire instructions | 失效指令，默认*Trigger.MISFIRE_INSTRUCTION_SMART_POLICY* |
| calendar             | 日历，常用于设置不生效的日期                             |



#### 2.4.2 SimpleTrigger

SimpleTrigger触发器适用于在指定时间执行任务，或者从指定时间开始每间隔多长时间执行一次任务的场景。常用属性有：

| 属性名         | 描述             |
| -------------- | ---------------- |
| startTime      | 开始生效时间     |
| endTime        | 失效时间         |
| repeatCount    | 重复次数         |
| repeatInterval | 任务间隔，单位ms |

可以使用`TriggerBuilder`和`SimpleScheduleBuilder`的静态方法构建SimpleTrigger。

```java
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.DateBuilder.*:
```

##### 1.使用示例

1. 特定时间执行一次

   ```java
   Date date = new Date();
   date.setTime(new Date().getTime() + 5000);
   Trigger trigger = newTrigger().withIdentity("trigger", "group")
       .startAt(date)
       .build();
   
   scheduler.scheduleJob(job, trigger);
   ```

2. 特定时间开始，执行十次，间隔十秒

   ```java
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
   ```

3. 在2分钟后执行一次

   ```java
   Trigger trigger = newTrigger()
       .withIdentity("trigger3", "group")
       .startAt(futureDate(2, IntervalUnit.MINUTE))
       .build();
   scheduler.scheduleJob(job, trigger);
   
   Thread.sleep(130000);
   ```

4. 现在开始执行，每隔五分钟一次，直到下午3点

   ```java
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
   ```

##### 2.失效指令

`SimpleTrigger`有七种可选的失效指令，类专用的有：

```
MISFIRE_INSTRUCTION_FIRE_NOW
MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT
MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT
MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT
MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
REPEAT_INDEFINITELY
```

还有父类提供的`MISFIRE_INSTRUCTION_SMART_POLICY`和`MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY`，默认采取的策略是`MISFIRE_INSTRUCTION_SMART_POLICY`，根据实例的属性动态选择具体措施。可以在创建实例时手动修改。



#### 2.4.3 CronTrigger

CroTrigger可以提供更加灵活的调度，如每周五的12点执行等，必须设置startTime属性，endTime属性可选。

##### 1.Cron表达式

需要使用cron表达式来对CronTrigger实例进行配置。Cron表达式由7个子表达式组成，中间使用空格分隔，分别代表：

- Seconds
- Minutes
- Hours
- Day-of-Month
- Month
- Day-of-Week
- Year（可选）

简单示例："0 0 12 ? \* WED" 表示每个星期三的12:00。一些常用的规则如下：

1. 每个子表达式可以包含范围或者列表，如周一至周五表示为"MON-FRI"，周一、三、五表示为"MON,WED,FRI"。

2. 通配符"\*"：表示该字段每个可能值。如 "\*" 用在Month字段上表示每个月，在Day-of-Month上表示每天。

3. 每个字段都有特定的合法值：

   1. Seconds和Minutes（0-59），
   2. Hours（0-23），
   3. Day-of-Month（0-31），
   4. Month（0-11或者使用字符串：JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC），
   5. Day-of-Week（1-7或者使用字符串：SUN, MON, TUE, WED, THU, FRI, SAT），1代表星期天，以此类推

4. 正斜线"/"代表递增步长

   例如在Minutes上使用"3/15"，表示该小时内从3分钟开始的每15分钟；"/35"表示从0开始的每35分钟。

5. "?"用在Day-of-Month和Day-of-Week字段上，表示不指定任何值，通常在需要指定其中一个字段的情况下，用在另外一个字段上。

6. "L"用在Day-of-Month和Day-of-Week字段上，表示最后一个值。如一月是31，一周是7，及周六。如果放在Day-of-Week的某个值后面，如FIRL，则表示这个月的最后一个星期五。另外还可以指定偏移量，如在Day-of-Month上设置"L-3"，则表示该月的倒数第三天。

7. "W"表示最接近某一天的工作日，如在Day-of-Month上设置"15W"，表示离当月第15天最近的工作日。

8. "#"用在Day-of-Week上来表达月的“第几个”星期几，如"6#3"或者"FRI#3"表示这个月的第三个星期五。



更多Cron表达式示例：

- “0 0/5 * * * ?”：从0开始的每5分钟
- “10 0/5 * * * ?”：每5分钟的第十秒
- “0 30 10-13 ? * WED,FRI”：周三和周五的10:30、11:30、12:30、13:30
- “0 0/30 8-9 5,20 * ?”：每月的第5和第20天的8:00、8:30、9:00、9:30

当调度要求太复杂，一个表达式不足以表达时，可以结合多个表达式。

##### 2.使用示例

CronTrigger实例可以通过TriggerBuilder和CronScheduleBuilder 来创建，静态导入可以方便使用：

```java

import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.DateBuilder.*:
```



1. 9点到18点的每个偶数分钟执行一次

   ```java
   Trigger trigger = newTrigger().withIdentity("trigger1", "group")
       .withSchedule(
       cronSchedule("0 0/2 9-18 * * ?")
   )
       .build();
   
   scheduler.scheduleJob(job, trigger);
   ```

2. 每天的16:30触发

   ```java
   Trigger trigger = newTrigger().withIdentity("trigger2", "group")
       .withSchedule(
       cronSchedule("0 30 16 * * ?")
   )
       .build();
   
   scheduler.scheduleJob(job, trigger);
   ```

3. 北京时间每周二的16:30触发

   ```java
   Trigger trigger = newTrigger().withIdentity("trigger2", "group")
       .withSchedule(
       cronSchedule("0 30 16 ? * TUE")
       .inTimeZone(TimeZone.getTimeZone("Asia/Shanghai"))
   )
       .build();
   
   scheduler.scheduleJob(job, trigger);
   ```

##### 3.失效指令

CronTrigger的失效指令包含：

```
MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
MISFIRE_INSTRUCTION_DO_NOTHING
MISFIRE_INSTRUCTION_FIRE_NOW
```

默认使用`MISFIRE_INSTRUCTION_FIRE_NOW`。



### 2.5 Listeners

监听器用根据调度器中发生的时间执行特定的操作。**TriggerListener**接收与触发器相关的事件，而**JobListeners** 接收与作业相关的事件，**SchedulerListener**接收与调度器自身的事件通知，如Job/Trigger的添加和移除等等。

- 与触发器相关的事件包括：触发器触发，触发器未触发和触发器完成（由触发器触发的作业已完成）。
- 与作业相关的事件包括：即将执行作业的通知，以及作业完成执行时的通知。

要自定义监听器，只需要实现`TriggerListener`或者`JobListener`以及`SchedulerListener`接口。为了方便，可以继承类`JobListenerSupport`或`TriggerListenerSupport`。

定义好监听器后，还需要将它注册到调度器中，如：

```java
scheduler.getListenerManager().addJobListener(myJobListener, KeyMatcher.jobKeyEquals(new JobKey("myJobName", "myJobGroup")));
```

需要同时使用Matcher来将决定接受来自哪个Job或者Trigger的事件。示例：

listener：

```java
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

```

```java
public class MyTriggerListener extends TriggerListenerSupport {
    @Override
    public String getName() {
        return "MyTriggerListener";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        TriggerKey key = trigger.getKey();
        System.err.println("Trigger{" + key +"} is fired...");
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        TriggerKey key = trigger.getKey();
        System.err.println("Trigger{" + key +"} has been fired successfully...");
    }
}
```

测试类：

```java
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
```



### 2.6 JobStore

JobStore负责存储你交给调度器的所有“工作数据”，包括：作业，触发器，日历等等。可以存储在内存中，也可以存储在数据库中。

#### 2.6.1.RAMStore

顾名思义，RAMStore就是将工作数据存储到RAM内存中。要使用RAMStore。只需要在quartz.properties配置如下信息：

```properties
org.quartz.jobStore.class = org.quartz.simpl.RAMJobStore
```



#### 2.6.2.JDBCJobStore

使用JDBCJobStore将会把工作数据存储在数据库中。配置要复杂一些。

##### 1.创建数据库和表

建表的SQL脚本在Quartz发行版的`docs/dbTables`目录下可以找到。

```mysql
create database quartz_learning;
use quartz_learning;

#
# In your Quartz properties file, you'll need to set 
# org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate
#
#
# By: Ron Cordell - roncordell
#  I didn't see this anywhere, so I thought I'd post it here. This is the script from Quartz to create the tables in a MySQL database, modified to use INNODB instead of MYISAM.

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

-- 作业定义表
CREATE TABLE QRTZ_JOB_DETAILS(
SCHED_NAME VARCHAR(120) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
JOB_CLASS_NAME VARCHAR(250) NOT NULL,
IS_DURABLE VARCHAR(1) NOT NULL,
IS_NONCONCURRENT VARCHAR(1) NOT NULL,
IS_UPDATE_DATA VARCHAR(1) NOT NULL,
REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP))
ENGINE=InnoDB;

-- 触发器表
CREATE TABLE QRTZ_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
NEXT_FIRE_TIME BIGINT(13) NULL,
PREV_FIRE_TIME BIGINT(13) NULL,
PRIORITY INTEGER NULL,
TRIGGER_STATE VARCHAR(16) NOT NULL,
TRIGGER_TYPE VARCHAR(8) NOT NULL,
START_TIME BIGINT(13) NOT NULL,
END_TIME BIGINT(13) NULL,
CALENDAR_NAME VARCHAR(200) NULL,
MISFIRE_INSTR SMALLINT(2) NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP))
ENGINE=InnoDB;

-- 简单触发器
CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
REPEAT_COUNT BIGINT(7) NOT NULL,
REPEAT_INTERVAL BIGINT(12) NOT NULL,
TIMES_TRIGGERED BIGINT(10) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

-- Cron触发器
CREATE TABLE QRTZ_CRON_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
CRON_EXPRESSION VARCHAR(120) NOT NULL,
TIME_ZONE_ID VARCHAR(80),
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;


CREATE TABLE QRTZ_SIMPROP_TRIGGERS
  (          
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) 
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
BLOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_CALENDARS (
SCHED_NAME VARCHAR(120) NOT NULL,
CALENDAR_NAME VARCHAR(200) NOT NULL,
CALENDAR BLOB NOT NULL,
PRIMARY KEY (SCHED_NAME,CALENDAR_NAME))
ENGINE=InnoDB;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
ENTRY_ID VARCHAR(95) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
FIRED_TIME BIGINT(13) NOT NULL,
SCHED_TIME BIGINT(13) NOT NULL,
PRIORITY INTEGER NOT NULL,
STATE VARCHAR(16) NOT NULL,
JOB_NAME VARCHAR(200) NULL,
JOB_GROUP VARCHAR(200) NULL,
IS_NONCONCURRENT VARCHAR(1) NULL,
REQUESTS_RECOVERY VARCHAR(1) NULL,
PRIMARY KEY (SCHED_NAME,ENTRY_ID))
ENGINE=InnoDB;

CREATE TABLE QRTZ_SCHEDULER_STATE (
SCHED_NAME VARCHAR(120) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
CHECKIN_INTERVAL BIGINT(13) NOT NULL,
PRIMARY KEY (SCHED_NAME,INSTANCE_NAME))
ENGINE=InnoDB;

CREATE TABLE QRTZ_LOCKS (
SCHED_NAME VARCHAR(120) NOT NULL,
LOCK_NAME VARCHAR(40) NOT NULL,
PRIMARY KEY (SCHED_NAME,LOCK_NAME))
ENGINE=InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME,JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME,CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);

commit; 

```



##### 2.配置数据源

```properties
# datasource config
org.quartz.dataSource.mysql.URL=jdbc:mysql://localhost:3306/quartz_learning?serverTimezone=Asia/Shanghai
org.quartz.dataSource.mysql.user=root
org.quartz.dataSource.mysql.password=root
org.quartz.dataSource.mysql.driver=com.mysql.cj.jdbc.Driver

```



##### 3.配置事务类型和驱动代理

```properties
# jdbcStore config
org.quartz.jobStore.class = org.quartz.impl.jdbcjobstore.JobStoreTX
org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate
org.quartz.jobStore.tablePrefix = QRTZ_	# 表前缀，取决于你数据库脚本
org.quartz.jobStore.dataSource = mysql	# 数据源名称，跟上一步配置一致
```

##### 4.添加数据库依赖

因为Quartz默认使用c3p0数据源实现，因此需添加pom依赖：

```xml
        <dependency>
            <groupId>com.mchange</groupId>
            <artifactId>c3p0</artifactId>
            <version>0.9.5.5</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.18</version>
        </dependency>
```

Quartz还提供了hikaricp支持，要使用的话只需添加数据源配置：

```properties
# 数据源实现：hikaricp或者c3p0(默认)
org.quartz.dataSource.mysql.provider=hikaricp   
```



##### 5.运行测试

```java
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
```

程序启动后，可以发现数据库中的job_detail，trigger等表都有了一条记录。

#### 2.6.3 TerracottaJobStore

待测试



### 2.7 Configuration和SchedulerFactory

要使用Quartz，必须配置如下主要的组件：

- ThreadPool 线程池
- JobStore 作业存储
- DataSources (if necessary) 数据源
- Scheduler 调度器

ThreadPool为任务执行提供工作线程，一般而言配置大小为5就足够使用了。JobStore和DataSource前面已经详细讲解。Scheduler需要为其定义一个名称。

#### 2.7.1 StdSchedulerFactory

StdSchedulerFactory是`org.quartz.SchedulerFactory`接口的实现。它使用一组属性（java.util.Properties）创建和初始化Quartz Scheduler。这些属性通常从文件中加载，但是也可以由程序创建并直接交给工厂。只需在工厂上调用`getScheduler()`即可生成调度器实例，对其进行初始化（同时初始化它的ThreadPool，JobStore和DataSources）。

#### 2.7.2 DirectSchedulerFactory

DirectSchedulerFactory是另一个SchedulerFactory实现。它可以通过Java代码来配置调度器，而不使用配置文件。通常不建议使用它，原因如下：（1）它要求用户对它有深入的了解，（2）它不允许进行声明式配置--即要对调度器配置进行硬编码。



### 2.8 其他参考文档

- [CookBook](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/cookbook/)
- [Examples](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/examples/)
- [Configuration](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/)



## 3. 框架整合

### 3.1 Quartz与Spring整合

`quartz.properties`配置文件：

```properties
# JDBCStore
# instance
org.quartz.scheduler.instanceName = MyScheduler
org.quartz.threadPool.threadCount = 5

# datasource config
org.quartz.dataSource.mysql.URL=jdbc:mysql://localhost:3306/quartz_learning?serverTimezone=Asia/Shanghai
org.quartz.dataSource.mysql.user=root
org.quartz.dataSource.mysql.password=root
org.quartz.dataSource.mysql.driver=com.mysql.cj.jdbc.Driver
# 数据源实现：hikaricp或者c3p0(默认)
org.quartz.dataSource.mysql.provider=hikaricp

# jdbcStore config
org.quartz.jobStore.class = org.quartz.impl.jdbcjobstore.JobStoreTX
org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate
org.quartz.jobStore.tablePrefix = QRTZ_
org.quartz.jobStore.dataSource = mysql
```

Java配置类：

```java
@Configuration
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
    public Scheduler stdScheduler(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.deleteJob(jobDetail.getKey());
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
        return scheduler;
    }

}
```

除了使用Quartz API创建Scheduler Bean，还可以使用Spring-context-support包中的类来创建：

需要添加依赖：

```xml
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context-support</artifactId>
            <version>xxx</version>
        </dependency>
```

注册Bean：

```java
   
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


```



### 3.2 Quartz与Spring Boot整合

添加依赖：

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </dependency>
```

自动配置类：

Spring Boot通过`org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration`自动配置类读取`application.yml`或者`application.properties`文件创建出`Scheduler`实例并将其注册到IOC容器中，根据配置立即或者延迟启动调度器。

可以修改`application.yml`进行自定义配置：

```yml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      schema: classpath:/tables_mysql_innodb.sql  # 数据库初始化脚本，可以不设置
      initialize-schema: always # 数据库初始化策略
    scheduler-name: QuartzScheduler
  datasource:
    url: jdbc:mysql://localhost:3306/quartz_learning?serverTimezone=Asia/Shanghai
    username: root
    password: root
```












