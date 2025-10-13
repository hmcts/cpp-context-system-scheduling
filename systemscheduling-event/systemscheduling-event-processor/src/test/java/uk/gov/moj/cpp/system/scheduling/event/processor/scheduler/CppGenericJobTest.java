package uk.gov.moj.cpp.system.scheduling.event.processor.scheduler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

@ExtendWith(MockitoExtension.class)
public class CppGenericJobTest {

    @InjectMocks
    private CppGenericJob job;

    @Mock
    private ScheduleJobProcessor scheduleJobProcessor;

    @Test
    public void execute() throws Exception {
        final JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
        final Map<String, String> jobData = new HashMap<>();
        jobData.put("jobDetails", "{}");
        final JobDetail jobDetail = JobBuilder.newJob()
                .withDescription("test job")
                .withIdentity(UUID.randomUUID().toString())
                .ofType(CppGenericJob.class)
                .usingJobData(new JobDataMap(jobData))
                .build();
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);


        job.execute(jobExecutionContext);

        verify(scheduleJobProcessor, times(1)).execute(anyString());

    }

}