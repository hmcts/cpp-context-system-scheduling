package uk.gov.moj.cpp.system.scheduling.command.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemJobSchedulingApiTest {

    @InjectMocks
    private SystemJobSchedulingApi systemJobSchedulingApi;
    @Mock
    private Sender sender;

    @Test
    public void scheduleJob() {

        final JsonEnvelope command = EnvelopeFactory.createEnvelope("systemscheduling.command.schedule-job"
                , Json.createObjectBuilder().add("cronExpression", "0 0/0 * * * ?").build());
        systemJobSchedulingApi.scheduleJob(command);
        verify(sender).send(command);
    }

    @Test
    public void scheduleJobWithBadCronExpression() throws Exception {
        //Given
        final JsonEnvelope command = EnvelopeFactory.createEnvelope("systemscheduling.command.schedule-job"
                , Json.createObjectBuilder().add("cronExpression", "-1 0/0 * * * ?").build());


        final BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> systemJobSchedulingApi.scheduleJob(command));

        assertThat(badRequestException.getMessage(), is("The cronExpression <-1 0/0 * * * ?> in the payload is invalid"));
    }

    @Test
    public void shouldHandleCommands() {
        assertThat(SystemJobSchedulingApi.class, isHandlerClass(COMMAND_API)
                .with(allOf(
                        method("cancelJob").thatHandles("systemscheduling.command.cancel-job").withSenderPassThrough(),
                        method("scheduleJob").thatHandles("systemscheduling.command.schedule-job")
                )));
    }
}