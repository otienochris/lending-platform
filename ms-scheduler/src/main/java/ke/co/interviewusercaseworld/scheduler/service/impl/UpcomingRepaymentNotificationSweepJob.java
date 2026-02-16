package ke.co.interviewusercaseworld.scheduler.service.impl;

import ke.co.interviewusercaseworld.commons.enums.LoanStatusEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.scheduler.repository.RepaymentScheduleRepository;
import ke.co.interviewusercaseworld.scheduler.service.SweepJob;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpcomingRepaymentNotificationSweepJob implements SweepJob {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    @Override
    public String getCron() {
        return "*/7 * * * * *";
    }

    @Override
    @Job(name = "Upcoming Repayment Notification Events", retries = 3)
    public void schedule(String currentSweepJob) {
        Helpers.log(currentSweepJob, LogLevelEnum.info, OperationNameEnum.OUTBOX_EVENTS_SCHEDUING, "Running " + currentSweepJob, null);
        repaymentScheduleRepository.findTop50ByStatusAndDueDateBetweenOrderByDueDateAsc(LoanStatusEnum.OPEN.name(), LocalDateTime.now(), LocalDateTime.now().plusMonths(3))
                .flatMap(repaymentSchedule -> {
                    Helpers.log("", LogLevelEnum.info, OperationNameEnum.REPAYMENT_NOTIFICATION, "Sending notification for almost due loans: Schedule Id: " + repaymentSchedule.getScheduleId() + " due on " + repaymentSchedule.getDueDate(), null);
                    return Mono.just(repaymentSchedule);
                }).subscribe();

    }

    @Override
    public void delete() {

    }
}
