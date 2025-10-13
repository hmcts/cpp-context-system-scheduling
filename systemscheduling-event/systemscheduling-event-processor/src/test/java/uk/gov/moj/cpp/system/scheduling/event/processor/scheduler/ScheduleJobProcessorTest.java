package uk.gov.moj.cpp.system.scheduling.event.processor.scheduler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.system.scheduling.event.processor.helpers.FileHelper.readJsonObject;
import static uk.gov.moj.cpp.system.scheduling.event.processor.matchers.EndpointDefinitionMatcher.matchEndpointDefinition;

import uk.gov.justice.services.adapter.rest.parameter.ParameterType;
import uk.gov.justice.services.clients.core.EndpointDefinition;
import uk.gov.justice.services.clients.core.QueryParam;
import uk.gov.justice.services.clients.core.RestClientProcessor;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;

import java.util.Optional;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class ScheduleJobProcessorTest {

    @Mock
    private RestClientProcessor jobRestClient;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<EndpointDefinition> endpointDefinitionArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @InjectMocks
    private ScheduleJobProcessor scheduleJobProcessor;

    @Test
    public void shouldCallRestClientWithQueryParams() {
        final String action = randomAlphanumeric(10);
        final JsonObject payload = readJsonObject("system.command.scheduling.schedule.json", action);
        scheduleJobProcessor.execute(payload.toString());
        verify(jobRestClient).post(endpointDefinitionArgumentCaptor.capture(), jsonEnvelopeArgumentCaptor.capture());

        assertThat(endpointDefinitionArgumentCaptor.getValue(), matchEndpointDefinition(getEndpointDefinition(payload)));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata(), metadata().withName(action));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().payloadAsJsonObject().toString(), isJson(allOf(
                withJsonPath("$.recordedLabel", equalTo("Hearing Started"))
        )));
    }

    private JsonEnvelopeMatcher hearingDefinitionQuery(final JsonEnvelope envelope) {
        return jsonEnvelope(
                withMetadataEnvelopedFrom(envelope),
                payloadIsJson(allOf(
                        withJsonPath("$.payload.xyz", equalTo("Test".toString()))
                )));
    }

    @Test
    public void shouldCallRestClientNoQueryParams() {
        final String action = randomAlphanumeric(10);
        final JsonObject payload = readJsonObject("system.command.schedule-with-no-query-params.json", action);
        scheduleJobProcessor.execute(payload.toString());
        verify(jobRestClient).post(endpointDefinitionArgumentCaptor.capture(), jsonEnvelopeArgumentCaptor.capture());

        assertThat(endpointDefinitionArgumentCaptor.getValue(), matchEndpointDefinition(getEndpointDefinition(payload)));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata(), metadata().withName(action));
        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata(), metadata().withName(action));
    }

    private EndpointDefinition getEndpointDefinition(final JsonObject payload) {
        final JsonObject endpointDefinition = payload.getJsonObject("parameters").getJsonObject("endpointDefinition");

        final Set<QueryParam> queryParams = Optional.ofNullable(endpointDefinition.getJsonArray("queryParams"))
                .orElse(Json.createArrayBuilder().build())
                .stream().map(p -> new QueryParam(p.toString(), false, ParameterType.STRING)).collect(toSet());

        return new EndpointDefinition(
                endpointDefinition.getString("baseUri"),
                endpointDefinition.getString("resource"),
                emptySet(),
                queryParams,
                endpointDefinition.getString("mediaType")
        );
    }

}
