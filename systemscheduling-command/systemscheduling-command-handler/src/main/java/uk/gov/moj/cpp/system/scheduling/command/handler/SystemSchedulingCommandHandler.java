package uk.gov.moj.cpp.system.scheduling.command.handler;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.scheduling.aggregate.Job;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class SystemSchedulingCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Handles("systemscheduling.command.schedule-job")
    public void scheduleJob(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID id = UUID.fromString(payload.getString("id"));
        final String name = payload.getString("name");
        final String description = payload.getString("description");
        final JsonObject parameters = payload.getJsonObject("parameters");
        final String type = payload.getString("type");
        final String cronExpression = payload.getString("cronExpression");

        final EventStream eventStream = eventSource.getStreamById(id);
        final Job job = aggregateService.get(eventStream, Job.class);
        final Stream<Object> events = job.scheduleJob(id, name, description, parameters, type, cronExpression);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    @Handles("systemscheduling.command.cancel-job")
    public void cancelJob(final JsonEnvelope command) throws EventStreamException {
        final UUID id = UUID.fromString(command.payloadAsJsonObject().getString("id"));
        final EventStream eventStream = eventSource.getStreamById(id);
        final Job job = aggregateService.get(eventStream, Job.class);
        final Stream<Object> events = job.cancelJob(id);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }
}
