package ke.co.interviewusercaseworld.scheduler.configs;

import jakarta.annotation.PostConstruct;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.scheduler.service.SweepJob;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class JobRunrSchedulerConfiguration {

    private final Map<String, SweepJob> sweepJobs;
    private final JobScheduler jobScheduler;


    @PostConstruct
    public void scheduleRecurringJobs() {

        sweepJobs.forEach((s, sweepJob) -> {
            Helpers.log(null, LogLevelEnum.INFO, OperationNameEnum.JOB_SCHEDULING, "Scheduling job " + s, null);
            jobScheduler.scheduleRecurrently(
                    s.replace("SweepJob", "") + "-processor",
                    sweepJob.getCron(),
                    () -> sweepJob.schedule(s)

            );
        });
    }
}
