package uk.gov.moj.cpp.system.scheduling.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.scheduling.event.processor.scheduler.CppJobScheduler;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;


@ExtendWith(MockitoExtension.class)
public class SystemSchedulingEventProcessorTest {

    @InjectMocks
    private SystemSchedulingEventProcessor systemSchedulingEventProcessor;

    @Mock
    private CppJobScheduler cppJobScheduler;

    private JsonEnvelope event;

    @Test
    public void jobCreated() throws SchedulerException {
        final JsonObject jobDetails = Json.createObjectBuilder().build();
        final JsonEnvelope event = envelope()
                .with(metadataBuilder()
                        .withId(randomUUID())
                        .withName("systemscheduling.job-scheduled"))
                .withPayloadFrom(jobDetails).build();
        systemSchedulingEventProcessor.jobScheduled(event);
        verify(cppJobScheduler).scheduleJob(jobDetails);
    }

    @Test
    public void jobCanceled() throws SchedulerException {
        final String jobId = "Job to cancel";
        final JsonObject jobDetails = Json.createObjectBuilder().add("id", jobId).build();
        final JsonEnvelope event = envelope()
                .with(metadataBuilder()
                        .withId(randomUUID())
                        .withName("systemscheduling.job-canceled"))
                .withPayloadFrom(jobDetails).build();
        systemSchedulingEventProcessor.jobCanceled(event);
        verify(cppJobScheduler).cancelJob(jobId);
    }

    @Test
    public void shouldHandleEvents() {
        assertThat(SystemSchedulingEventProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(allOf(
                        method("jobScheduled").thatHandles("systemscheduling.job-scheduled"),
                        method("jobCanceled").thatHandles("systemscheduling.job-canceled")
                )));
    }
}
