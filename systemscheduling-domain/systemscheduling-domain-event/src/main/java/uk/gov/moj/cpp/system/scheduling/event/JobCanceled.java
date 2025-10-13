package uk.gov.moj.cpp.system.scheduling.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("systemscheduling.job-canceled")
public class JobCanceled {

    private final UUID id;

    @JsonCreator
    public JobCanceled(@JsonProperty("id") final UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

}
