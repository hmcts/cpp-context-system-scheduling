package uk.gov.moj.cpp.system.scheduling.event.processor.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.spi.TriggerFiredBundle;

@ExtendWith(MockitoExtension.class)
public class CdiJobFactoryTest {

    private CdiJobFactory cdiJobFactory = new CdiJobFactory();
    @Mock
    private CDIProvider cdiProvider;
    @Mock
    private CDI cdi;
    @Mock
    private Job job;
    @Mock
    private Instance<Job> jobInstance;

    @Mock
    private CppGenericJob cppGenericJob;
    @Mock
    private TriggerFiredBundle triggerFiredBundle;

    @Test
    public void newJob() throws Exception {

        when(cdiProvider.getCDI()).thenReturn(cdi);
        when(jobInstance.get()).thenReturn(job);
        when(cdi.select(any(Class.class))).thenReturn(jobInstance);
        CDI.setCDIProvider(cdiProvider);
        final JobDetail jobDetail = JobBuilder.newJob()
                .withIdentity("test")
                .ofType(CppGenericJob.class)
                .build();
        when(triggerFiredBundle.getJobDetail()).thenReturn(jobDetail);

        final Job theJob = cdiJobFactory.newJob(triggerFiredBundle, null);
        assertEquals(job, theJob);

    }

}