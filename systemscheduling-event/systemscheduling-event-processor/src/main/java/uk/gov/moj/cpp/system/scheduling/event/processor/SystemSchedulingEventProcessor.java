package uk.gov.moj.cpp.system.scheduling.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.scheduling.event.processor.scheduler.CppJobScheduler;

import javax.inject.Inject;

import org.quartz.SchedulerException;

@ServiceComponent(EVENT_PROCESSOR)
public class SystemSchedulingEventProcessor {

    @Inject
    private CppJobScheduler cppJobScheduler;

    @Handles("systemscheduling.job-scheduled")
    public void jobScheduled(final JsonEnvelope event) {
        cppJobScheduler.scheduleJob(event.payloadAsJsonObject());
    }

    @Handles("systemscheduling.job-canceled")
    public void jobCanceled(final JsonEnvelope event) throws SchedulerException {
        final String jobId = event.payloadAsJsonObject().getString("id");
        cppJobScheduler.cancelJob(jobId);
    }

}
