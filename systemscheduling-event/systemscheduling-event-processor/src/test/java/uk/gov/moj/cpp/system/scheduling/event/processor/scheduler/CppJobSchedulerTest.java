package uk.gov.moj.cpp.system.scheduling.event.processor.scheduler;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

@ExtendWith(MockitoExtension.class)
public class CppJobSchedulerTest {

    static {
        System.setProperty("org.quartz.properties", "quartz-test.properties");
    }

    @InjectMocks
    private CppJobScheduler cppJobScheduler;
    @Mock
    private CdiJobFactory cdiJobFactory;

    @BeforeEach
    public void setUp() {
        cppJobScheduler.start();
    }

    @AfterEach
    public void tearDown() throws Exception {

        final Collection<Scheduler> allSchedulers = new StdSchedulerFactory().getAllSchedulers();
        if (allSchedulers.isEmpty()) {
            return;
        }
        final Scheduler scheduler = allSchedulers.iterator().next();
        scheduler.clear();
        scheduler.shutdown();
    }

    @Test
    public void shouldStartScheduler() throws SchedulerException {

        //already started on setup method

        final Collection<Scheduler> schedulers = new StdSchedulerFactory().getAllSchedulers();
        assertEquals(1, schedulers.size());
        final Scheduler scheduler = schedulers.iterator().next();
        assertTrue(scheduler.isStarted());
    }

    @Test
    public void shouldStopScheduler() throws SchedulerException {

        cppJobScheduler.stop();

        final Collection<Scheduler> schedulers = new StdSchedulerFactory().getAllSchedulers();
        assertEquals(0, schedulers.size());

    }

    @Test
    public void shouldScheduleAndCancelJob() throws Exception {

        final String jobId = randomUUID().toString();
        final String description = "Generates reports for the cases";
        final String name = "MIReport";
        final String cronExpression = "0 0/2 * * * ?";

        final JsonEnvelope jobCommand = envelope().with(metadataBuilder()
                .withId(randomUUID())
                .withName("mi.command.generate-case-report"))
                .build();

        final JsonObject endpointDefinition = Json.createObjectBuilder()
                .add("baseUri", "http://localhost:8080/mi-command-api/command/api/rest/mi")
                .add("resource", "/generate-case-report")
                .add("mediaType", "mi.command.generate-case-report")
                .build();

        final JsonObject jobDetails = Json.createObjectBuilder().add("id", jobId)
                .add("name", name)
                .add("description", description)
                .add("type", "REST")
                .add("parameters", Json.createObjectBuilder()
                        .add("endpointDefinition", endpointDefinition)
                        .add("method", "POST")
                        .add("envelope", jobCommand.asJsonObject())
                )
                .add("cronExpression", cronExpression)
                .build();


        cppJobScheduler.scheduleJob(jobDetails);

        final Scheduler scheduler = new StdSchedulerFactory().getAllSchedulers().iterator().next();

        final Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
        final JobKey jobKey = jobKeys.iterator().next();
        assertThat(jobKeys, hasSize(1));
        assertThat(jobKey.getName(), equalTo(jobId));

        final JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        assertThat(jobDetail.getDescription(), equalTo(description));
        assertThat(jobDetail.getJobClass().getSimpleName(), equalTo("CppGenericJob"));
        assertThat(jobDetail.getJobDataMap().getString("jobDetails"), equalTo(jobDetails.toString()));

        final List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        final CronTrigger cronTrigger = (CronTrigger) triggers.get(0);
        final TriggerKey triggerKey = cronTrigger.getKey();
        assertThat(triggers, hasSize(1));
        assertThat(cronTrigger.getCronExpression(), equalTo(cronExpression));
        assertThat(triggerKey.getName(), equalTo(name));
        assertThat(triggerKey.getGroup(), equalTo("CPPGroup"));

        cppJobScheduler.cancelJob(jobId);

        assertThat(scheduler.getJobDetail(jobKey), nullValue());
    }

}
