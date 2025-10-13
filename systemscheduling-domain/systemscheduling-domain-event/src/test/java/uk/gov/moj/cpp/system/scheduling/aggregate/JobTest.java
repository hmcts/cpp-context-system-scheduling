package uk.gov.moj.cpp.system.scheduling.aggregate;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.system.scheduling.event.JobCanceled;
import uk.gov.moj.cpp.system.scheduling.event.JobScheduled;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobTest {

    private Job job;

    private final static UUID id = UUID.randomUUID();
    private final static String name = "Daily report";
    private final static String description = "Generates daily report";
    private final static JsonObject parameters = Json.createObjectBuilder().build();
    private final static String type = "REST";
    private final static String cronExpression1 = "0 30 21 * * ?";
    private final static String cronExpression2 = "0 0 21 * * ?";

    @BeforeEach
    public void init() {
        job = new Job();
    }

    @Test
    public void shouldScheduleJob() {
        final Stream<Object> streamOfEvents = job.scheduleJob(id, name, description, parameters, type, cronExpression1);
        final List<Object> events = streamOfEvents.collect(Collectors.toList());

        assertThat(events, hasSize(1));

        final JobScheduled jobScheduled = (JobScheduled) events.get(0);
        assertThat(jobScheduled.getId(), equalTo(id));
        assertThat(jobScheduled.getName(), equalTo(name));
        assertThat(jobScheduled.getDescription(), equalTo(description));
        assertThat(jobScheduled.getType(), equalTo(type));
        assertThat(jobScheduled.getParameters(), equalTo(parameters));
        assertThat(jobScheduled.getCronExpression(), equalTo(cronExpression1));
    }

    @Test
    public void shouldRescheduleJob() {
        job.scheduleJob(id, name, description, parameters, type, cronExpression1);

        final Stream<Object> streamOfEvents = job.scheduleJob(id, name, description, parameters, type, cronExpression2);
        final List<Object> events = streamOfEvents.collect(Collectors.toList());

        assertThat(events, hasSize(1));

        final JobScheduled jobScheduled = (JobScheduled) events.get(0);
        assertThat(jobScheduled.getId(), equalTo(id));
        assertThat(jobScheduled.getName(), equalTo(name));
        assertThat(jobScheduled.getDescription(), equalTo(description));
        assertThat(jobScheduled.getType(), equalTo(type));
        assertThat(jobScheduled.getParameters(), equalTo(parameters));
        assertThat(jobScheduled.getCronExpression(), equalTo(cronExpression2));
    }

    @Test
    public void shouldCancelScheduledJob() {
        job.scheduleJob(id, name, description, parameters, type, cronExpression1).collect(Collectors.toList());
        final Stream<Object> streamOfEvents = job.cancelJob(id);
        final List<Object> events = streamOfEvents.collect(Collectors.toList());

        assertThat(events, hasSize(1));

        final JobCanceled jobCanceled = (JobCanceled) events.get(0);

        assertThat(jobCanceled.getId(), equalTo(id));
    }

    @Test
    public void shouldNotCancelNotScheduledJob() {
        final UUID id = UUID.randomUUID();

        final List<Object> events = job.cancelJob(id).collect(Collectors.toList());

        assertThat(events.isEmpty(), equalTo(true));
    }

}
