package uk.gov.moj.cpp.system.scheduling.event.processor.scheduler;

import static org.quartz.CronScheduleBuilder.cronSchedule;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class CppJobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CppJobScheduler.class);

    private Scheduler scheduler;

    @Inject
    private JobFactory cdiJobFactory;

    @PostConstruct
    public void start() {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.setJobFactory(cdiJobFactory);
            scheduler.start();
            LOGGER.info("Starting scheduler .....");
        } catch (SchedulerException e) {
            LOGGER.error("Error initialising/starting the scheduler", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            try {
                LOGGER.info("Closing scheduler ......");
                scheduler.shutdown(false);
            } catch (SchedulerException e) {
                LOGGER.warn("Error while closing scheduler", e);
            }
        }
    }

    public void scheduleJob(final JsonObject jobDetails) {
        final JobDataMap parameters = new JobDataMap();
        parameters.put("jobDetails", jobDetails.toString());

        final JobDetail jobDetail = JobBuilder.newJob()
                .withDescription(jobDetails.getString("description"))
                .withIdentity(jobDetails.getString("id"))
                .ofType(CppGenericJob.class)
                .usingJobData(parameters)
                .build();

        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobDetails.getString("name"), "CPPGroup")
                .withSchedule(cronSchedule(jobDetails.getString("cronExpression")))
                .forJob(jobDetail)
                .build();

        final Set<Trigger> triggerSet = new HashSet<>();
        triggerSet.add(trigger);
        try {
            scheduler.scheduleJob(jobDetail, triggerSet, true);
        } catch (SchedulerException e) {
            LOGGER.error("Error in scheduling the scheduler", e);
        }
    }

    public boolean cancelJob(final String id) throws SchedulerException {
        final JobKey jobKey = new JobKey(id);
        return scheduler.deleteJob(jobKey);
    }

}
