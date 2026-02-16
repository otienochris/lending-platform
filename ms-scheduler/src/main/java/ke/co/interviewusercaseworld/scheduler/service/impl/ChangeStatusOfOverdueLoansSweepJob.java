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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChangeStatusOfOverdueLoansSweepJob implements SweepJob {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    @Override
    public String getCron() {
        return "*/7 * * * * *";
    }

    @Override
    @Job(name = "Overdue loans status update", retries = 3)
    public void schedule(String currentSweepJob) {
        Helpers.log(currentSweepJob, LogLevelEnum.info, OperationNameEnum.OUTBOX_EVENTS_SCHEDUING, "Running " + currentSweepJob, null);
        repaymentScheduleRepository.findTop50ByStatusNotAndDueDateBefore(LoanStatusEnum.CLOSED.name(), LocalDateTime.now())
                .flatMap(repaymentSchedule -> {
                    Helpers.log("", LogLevelEnum.info, OperationNameEnum.REPAYMENT_NOTIFICATION, "Changing status of overdue loan: Schedule Id: " + repaymentSchedule.getScheduleId() + " due on " + repaymentSchedule.getDueDate(), null);

                    repaymentSchedule.setStatus(LoanStatusEnum.OVERDUE.name());
                    //todo: apply charges
                    return repaymentScheduleRepository.save(repaymentSchedule);
                }).subscribe();

    }

    @Override
    public void delete() {

    }
}
