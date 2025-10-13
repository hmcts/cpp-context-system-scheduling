package uk.gov.moj.cpp.system.scheduling.event.processor.scheduler;

import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class CppGenericJob implements Job {

    @Inject
    private ScheduleJobProcessor processor;

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        final String jobDetailsAsJson = jobDataMap.getString("jobDetails");
        processor.execute(jobDetailsAsJson);
    }
}
