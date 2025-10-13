package uk.gov.moj.cpp.system.scheduling.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("systemscheduling.job-scheduled")
public class JobScheduled {

    private final UUID id;
    private final String name;
    private final String description;
    private final JsonObject parameters;
    private final String type;
    private final String cronExpression;

    @JsonCreator
    public JobScheduled(final UUID id, final String name,
                        final String description, final JsonObject parameters, final String type,
                        final String cronExpression) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.type = type;
        this.cronExpression = cronExpression;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonObject getParameters() {
        return parameters;
    }

    public String getType() {
        return type;
    }

    public String getCronExpression() {
        return cronExpression;
    }
}
