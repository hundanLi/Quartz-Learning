package com.tcl.quartz.listener;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.listeners.TriggerListenerSupport;

/**
 * @author li
 * @version 1.0
 * @date 2020/8/18 19:29
 */
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
