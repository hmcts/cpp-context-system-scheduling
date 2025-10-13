package uk.gov.moj.system.scheduling.it.test.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.core.ConditionTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;


public class WiremockHelper {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    public static final String MI_GENERATE_CASE_REPORT= "/mi-service/command/api/rest/mi/generate-case-report";

    public static final String MI_GENERATE_CASE_REPORT_CONTENT_TYPE = "application/vnd.mi.command.generate-case-report+json";

    public static void resetWiremock() {
        configureFor(HOST, 8080);
        reset();
    }

    public static void stubAccessControl(final String... groupNames) {

        final JsonArrayBuilder groupsArray = Json.createArrayBuilder();
        for (final String groupName : groupNames) {
            groupsArray.add(createObjectBuilder().add("groupId", UUID.randomUUID().toString()).add("groupName", groupName));
        }

        final JsonObject response = createObjectBuilder()
                .add("groups", groupsArray).build();

        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(response.toString())));
    }

    public static void stubRequest(final String url, final Response.Status status) {
        stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(status.getStatusCode())));
    }

    public static void verifyRequestSent(final String url, final String contentType) {
        waitAtMost(Duration.ofSeconds(30))
                .until(() -> verifyPostRequestedFor(url, contentType));

    }

    private static boolean verifyPostRequestedFor(final String url, final String contentType) {
        verify(postRequestedFor(urlEqualTo(url)).withHeader(CONTENT_TYPE, equalTo(contentType)));

        return true;
    }

    public static void verifyRequestNotSent(final String url, final String contentType) {
        try {
            verifyRequestSent(url, contentType);
            fail();
        } catch (final ConditionTimeoutException ignored) {

        }
    }

    public static void verifyMiPostWorkTriggered(final String jobId) {
        waitAtMost(Duration.ofSeconds(30)).until(() ->
                {
                    final Stream<JSONObject> payLoadStream = getMiGenerateCaseReport();
                    return payLoadStream
                            .anyMatch(
                                    payload -> {
                                        try {
                                            return payload.getString("caseId").equals(jobId);
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            );
                }

        );

    }

    private static Stream<JSONObject> getMiGenerateCaseReport() throws Exception {
        return findAll(postRequestedFor(urlPathEqualTo(MI_GENERATE_CASE_REPORT))
                .withHeader(CONTENT_TYPE, equalTo(MI_GENERATE_CASE_REPORT_CONTENT_TYPE)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(body -> {
                    try {
                        return new JSONObject(body);
                    } catch (JSONException e) {
                        throw new RuntimeException("Failed converting wiremock body to JSONObject", e);
                    }
                });
    }
}
