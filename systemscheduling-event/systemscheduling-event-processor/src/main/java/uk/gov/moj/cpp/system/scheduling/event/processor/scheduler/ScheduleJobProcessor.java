package uk.gov.moj.cpp.system.scheduling.event.processor.scheduler;


import static java.util.Collections.emptySet;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilderFrom;

import uk.gov.justice.services.adapter.rest.parameter.ParameterType;
import uk.gov.justice.services.clients.core.DefaultRestClientHelper;
import uk.gov.justice.services.clients.core.EndpointDefinition;
import uk.gov.justice.services.clients.core.QueryParam;
import uk.gov.justice.services.clients.core.RestClientProcessor;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;

@Stateless
public class ScheduleJobProcessor {

    @Inject
    private RestClientProcessor jobRestClient;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    public void execute(final String payloadAsString) {
        final JsonObject payload = stringToJsonObjectConverter.convert(payloadAsString);
        final JsonObject parameters = payload.getJsonObject("parameters");
        final EndpointDefinition endpointDefinition = getEndpointDefinition(parameters.getJsonObject("endpointDefinition"));

        final Metadata metadata = metadataBuilderFrom(createObjectBuilderWithFilter(
                parameters.getJsonObject("envelope").getJsonObject("_metadata"),
                anObject -> !"id".equals(anObject))
                .add("id", UUID.randomUUID().toString())
                .build())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadata,
                parameters.getJsonObject("envelope")
                        .getOrDefault("payload",
                                createObjectBuilder().build()));

        jobRestClient.post(endpointDefinition, envelope);
    }

    private EndpointDefinition getEndpointDefinition(final JsonObject payload) {
        return new EndpointDefinition(
                payload.getString("baseUri"),
                payload.getString("resource"),
                new DefaultRestClientHelper().extractPathParametersFromPath(payload.getString("resource")),
                getQueryParam(payload),
                payload.getString("mediaType")
        );
    }

    private Set<QueryParam> getQueryParam(final JsonObject payload) {
        if (!payload.containsKey("queryParams")) {
            return emptySet();
        }
        return payload.getJsonArray("queryParams")
                .stream()
                .map(jsonValue -> new QueryParam(jsonValue.toString(), false, ParameterType.STRING))
                .collect(Collectors.toSet());
    }
}
