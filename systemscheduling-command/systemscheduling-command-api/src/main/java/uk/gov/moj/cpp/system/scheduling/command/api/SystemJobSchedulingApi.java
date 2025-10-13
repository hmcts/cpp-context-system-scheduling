package uk.gov.moj.cpp.system.scheduling.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.quartz.CronExpression;

@ServiceComponent(COMMAND_API)
public class SystemJobSchedulingApi {

    @Inject
    private Sender sender;

    @Handles("systemscheduling.command.schedule-job")
    public void scheduleJob(final JsonEnvelope envelope) {
        final String cronExpression = envelope.payloadAsJsonObject().getString("cronExpression");
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new BadRequestException(String.format("The cronExpression <%s> in the payload is invalid", cronExpression));
        }
        sender.send(envelope);
    }

    @Handles("systemscheduling.command.cancel-job")
    public void cancelJob(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
