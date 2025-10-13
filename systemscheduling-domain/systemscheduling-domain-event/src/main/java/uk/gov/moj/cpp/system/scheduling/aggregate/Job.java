package uk.gov.moj.cpp.system.scheduling.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.system.scheduling.event.JobCanceled;
import uk.gov.moj.cpp.system.scheduling.event.JobScheduled;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Job implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(Job.class);

    private boolean scheduled;

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(JobScheduled.class).apply(o -> this.scheduled = true),
                when(JobCanceled.class).apply(o -> this.scheduled = false)
        );
    }

    public Stream<Object> scheduleJob(final UUID id, final String name, final String description, final JsonObject parameters, final String type, final String cronExpression) {
        final JobScheduled jobScheduled = new JobScheduled(id, name, description, parameters, type, cronExpression);
        return apply(Stream.of(jobScheduled));
    }

    public Stream<Object> cancelJob(final UUID id) {
        if (scheduled) {
            return apply(Stream.of(new JobCanceled(id)));
        } else {
            LOGGER.warn("Cannot cancel non scheduled job {}", id);
            return apply(Stream.empty());
        }
    }


}
