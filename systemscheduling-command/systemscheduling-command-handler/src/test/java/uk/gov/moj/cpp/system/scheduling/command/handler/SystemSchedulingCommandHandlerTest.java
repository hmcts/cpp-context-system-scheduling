package uk.gov.moj.cpp.system.scheduling.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.scheduling.aggregate.Job;
import uk.gov.moj.cpp.system.scheduling.event.JobCanceled;
import uk.gov.moj.cpp.system.scheduling.event.JobScheduled;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemSchedulingCommandHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private Job job;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(JobScheduled.class, JobCanceled.class);

    @InjectMocks
    private SystemSchedulingCommandHandler systemSchedulingCommandHandler;

    @Test
    public void shouldScheduleJob() throws Exception {

        final UUID jobId = UUID.randomUUID();
        final String name = "MIReport";
        final String description = "Generates reports for the cases";
        final JsonObject parameters = Json.createObjectBuilder().build();
        final String type = "REST";
        final String cronExpression = "0 0/0 * * * ?";

        final JsonEnvelope command = envelope()
                .with(metadataBuilder()
                        .withId(randomUUID())
                        .withName("systemscheduling.command.schedule-job"))
                .withPayloadOf(jobId.toString(), "id")
                .withPayloadOf(name, "name")
                .withPayloadOf(description, "description")
                .withPayloadOf(parameters, "parameters")
                .withPayloadOf(type, "type")
                .withPayloadOf(cronExpression, "cronExpression")
                .build();

        final JobScheduled jobScheduled = new JobScheduled(jobId, name, description, parameters, type, cronExpression);

        when(eventSource.getStreamById(jobId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Job.class)).thenReturn(job);
        when(job.scheduleJob(jobId, name, description, parameters, type, cronExpression)).thenReturn(Stream.of(jobScheduled));

        systemSchedulingCommandHandler.scheduleJob(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("systemscheduling.job-scheduled"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.id", equalTo(jobId.toString())),
                                        withJsonPath("$.name", equalTo(name)),
                                        withJsonPath("$.description", equalTo(description)),
                                        withJsonPath("$.parameters", equalTo(parameters)),
                                        withJsonPath("$.type", equalTo(type)),
                                        withJsonPath("$.cronExpression", equalTo(cronExpression))

                                ))
                        )
                )
        ));
    }

    @Test
    public void shouldCancelJob() throws Exception {
        final UUID jobId = UUID.randomUUID();

        final JsonEnvelope command = envelope()
                .with(metadataBuilder()
                        .withId(randomUUID())
                        .withName("systemscheduling.command.cancel-job"))
                .withPayloadOf(jobId.toString(), "id")
                .build();

        final JobCanceled jobCanceled = new JobCanceled(jobId);

        when(eventSource.getStreamById(jobId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Job.class)).thenReturn(job);
        when(job.cancelJob(jobId)).thenReturn(Stream.of(jobCanceled));

        systemSchedulingCommandHandler.cancelJob(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command).withName("systemscheduling.job-canceled"),
                                payloadIsJson(withJsonPath("$.id", equalTo(jobId.toString())))
                        )
                )
        ));
    }

}
