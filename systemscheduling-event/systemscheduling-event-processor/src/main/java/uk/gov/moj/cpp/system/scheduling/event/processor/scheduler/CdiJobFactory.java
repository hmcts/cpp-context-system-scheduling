package uk.gov.moj.cpp.system.scheduling.event.processor.scheduler;

import javax.enterprise.inject.spi.CDI;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

public class CdiJobFactory implements JobFactory {

    @Override
    public Job newJob(final TriggerFiredBundle triggerFiredBundle, final Scheduler scheduler) throws SchedulerException {
        return CDI.current().select(triggerFiredBundle.getJobDetail().getJobClass()).get();
    }
}
