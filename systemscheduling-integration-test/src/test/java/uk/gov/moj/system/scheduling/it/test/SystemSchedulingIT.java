package uk.gov.moj.system.scheduling.it.test;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.system.scheduling.it.test.helper.WiremockHelper.MI_GENERATE_CASE_REPORT;
import static uk.gov.moj.system.scheduling.it.test.helper.WiremockHelper.MI_GENERATE_CASE_REPORT_CONTENT_TYPE;
import static uk.gov.moj.system.scheduling.it.test.helper.WiremockHelper.resetWiremock;
import static uk.gov.moj.system.scheduling.it.test.helper.WiremockHelper.stubAccessControl;
import static uk.gov.moj.system.scheduling.it.test.helper.WiremockHelper.stubRequest;
import static uk.gov.moj.system.scheduling.it.test.helper.WiremockHelper.verifyMiPostWorkTriggered;

import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.system.scheduling.it.test.helper.WiremockHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class SystemSchedulingIT {

    private static final String BASE_URI = System.getProperty("baseUri", "http://localhost:8080");

    private Connection connection;
    private UUID jobId;

    @BeforeClass
    public static void init() throws ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
    }

    @BeforeEach
    public void setUp() throws Exception {
        jobId = UUID.randomUUID();
        resetWiremock();

        stubPingFor("systemscheduling-query-api");
        stubPingFor("mi-command-api");
        stubPingFor("usersgroups-query-api");

        stubAccessControl("System Users");
        stubRequest(MI_GENERATE_CASE_REPORT, ACCEPTED);

        dbSetup();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void shouldScheduleTriggerAndCancelJob() throws Exception {
        final Response response = scheduleJob(getPayload("/systemscheduling.command.schedule-job.json", jobId.toString()));

        assertThat(response.getStatus(), equalTo(ACCEPTED.getStatusCode()));

        verifyJobDetails(jobId);
        verifyTriggerDetails();

        waitForFirstTrigger();

        verifyMiPostWorkTriggered(jobId.toString());

        cancelJob(jobId);

        verifyJobDoesNotExist(jobId);
        verifyTriggerDoesNotExists();
    }

    @Test
    public void shouldSendBadRequestForInvalidCronExpression() {
        final Response response = scheduleJob(getPayload("/systemscheduling.command.schedule-job_bad_cron_expression.json", jobId.toString()));

        assertThat(response.getStatus(), equalTo(BAD_REQUEST.getStatusCode()));
    }

    private Response scheduleJob(final String definition) {
        final RestClient restClient = new RestClient();

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(USER_ID, randomUUID().toString());

        return restClient.postCommand(BASE_URI + "/systemscheduling-command-api/command/api/rest/systemscheduling/schedule-job",
                "application/vnd.systemscheduling.command.schedule-job+json", definition, headers);
    }

    private Response cancelJob(final UUID jobId) {
        final RestClient restClient = new RestClient();

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(USER_ID, randomUUID().toString());

        return restClient.postCommand(BASE_URI + "/systemscheduling-command-api/command/api/rest/systemscheduling/cancel-job/" + jobId,
                "application/vnd.systemscheduling.command.cancel-job+json", "", headers);
    }

    @SuppressWarnings("squid:S2925")
    private void waitForFirstTrigger() {
        try {
            Thread.sleep(2000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void verifyJobTriggered() {
        WiremockHelper.verifyRequestSent(MI_GENERATE_CASE_REPORT, MI_GENERATE_CASE_REPORT_CONTENT_TYPE);
    }

    private void verifyJobNotTriggered() {
        WiremockHelper.verifyRequestNotSent(MI_GENERATE_CASE_REPORT, MI_GENERATE_CASE_REPORT_CONTENT_TYPE);
    }

    private void verifyTriggerDetails() throws Exception {
        final Map<String, Object> triggerDetails = getTriggers(triggers -> !triggers.isEmpty());

        assertThat(triggerDetails, allOf(
                hasEntry("sched_name", "CPPScheduler"),
                hasEntry("trigger_name", "TestSchedulerJob"),
                hasEntry("cron_expression", "0/2 * * * * ?")
        ));
    }

    private void verifyTriggerDoesNotExists() throws Exception {
        final Map<String, Object> triggerDetails = getTriggers(triggers -> triggers.isEmpty());
        assertThat(triggerDetails.entrySet(), empty());
    }

    private void verifyJobDetails(final UUID jobId) throws Exception {
        final Map<String, Object> jobDetails = getJobDetails(jobId, jobs -> !jobs.isEmpty());

        assertThat(jobDetails, allOf(
                hasEntry("sched_name", "CPPScheduler"),
                hasEntry("job_name", jobId.toString()),
                hasEntry("job_class_name", "uk.gov.moj.cpp.system.scheduling.event.processor.scheduler.CppGenericJob")
        ));
    }

    private void verifyJobDoesNotExist(final UUID jobId) throws Exception {
        final Map<String, Object> jobDetails = getJobDetails(jobId, jobs -> jobs.isEmpty());
        assertThat(jobDetails.entrySet(), empty());
    }

    private Map<String, Object> getJobDetails(final UUID jobId, final Predicate<Map<String, Object>> predicate) throws Exception {
        return DbQuery.newQuery()
                .selectAll()
                .from("qrtz_job_details")
                .filterOn("job_name")
                .withParameters(jobId.toString())
                .queryWithRetry(connection, 5, 2000, predicate);
    }

    private Map<String, Object> getTriggers(final Predicate<Map<String, Object>> predicate) throws Exception {
        return DbQuery.newQuery()
                .selectAll()
                .from("qrtz_cron_triggers")
                .filterOn("trigger_name")
                .withParameters("TestSchedulerJob")
                .queryWithRetry(connection, 5, 2000, predicate);
    }

    private void dbSetup() throws Exception {
        connection = new TestJdbcConnectionProvider().getViewStoreConnection("systemscheduling");
        new DatabaseCleaner().cleanViewStoreTables("systemscheduling", "qrtz_cron_triggers", "qrtz_job_details");
    }

    private String getPayload(final String filePath, String value) {
        try {
            final String template = new String(Files.readAllBytes(Paths.get(this.getClass().getResource(filePath).toURI())));
            return template.replaceAll("JOB_ID", value);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Unable to load resource" + e);
        }
    }

}
