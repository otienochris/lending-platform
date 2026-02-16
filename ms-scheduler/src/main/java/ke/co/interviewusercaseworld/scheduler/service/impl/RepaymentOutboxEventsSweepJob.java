package ke.co.interviewusercaseworld.scheduler.service.impl;

import ke.co.interviewusercaseworld.commons.configs.AppProperties;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.scheduler.configs.SchedulerConfigsProperties;
import ke.co.interviewusercaseworld.scheduler.model.entity.RepaymentOutboxEvent;
import ke.co.interviewusercaseworld.scheduler.repository.RepaymentOutBoxEventRepository;
import ke.co.interviewusercaseworld.scheduler.service.SweepJob;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static ke.co.interviewusercaseworld.scheduler.utils.HelperUtils.processEvent;

@Service
@RequiredArgsConstructor
public class RepaymentOutboxEventsSweepJob implements SweepJob {


    private final RepaymentOutBoxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafka;
    private final SchedulerConfigsProperties appProperties;


    @Override
    public String getCron() {
//        return Cron.every30seconds();
        return "*/7 * * * * *"; //TODO: externalize
    }

    @Override
    @Job(name = "Repayment Outbox Events", retries = 3)
    public void schedule(String currentSweepJob) {

        Helpers.log(currentSweepJob, LogLevelEnum.info, OperationNameEnum.OUTBOX_EVENTS_SCHEDUING, "Running " + currentSweepJob, null);

        try {
            AppProperties.KafkaConfigs kafkaConfigs = appProperties.getServiceProperties().getSchedulerProperties().getKafkaConfigs();
            outboxEventRepository.findTop20ByIsPublishedFalse()
                    .flatMap(event -> processEvent(currentSweepJob, event.getEventType(), event.getAggregateId(), event.getPayload(), kafkaConfigs.getTopicsForCommand(), kafka)
                            .flatMap(isPublished -> isPublished ? markAsPublished(currentSweepJob, event) : Mono.empty())
                            .onErrorResume(error -> {
                                Helpers.log(currentSweepJob, LogLevelEnum.error, OperationNameEnum.OUTBOX_EVENTS_SCHEDUING, "Failed to process event ID: {}" + event.getId(), new RuntimeException(error));
                                return Mono.error(error);
                            })
                    )
                    .then()
                    .doOnSuccess((it) -> Helpers.log(currentSweepJob, LogLevelEnum.info, OperationNameEnum.OUTBOX_EVENTS_SCHEDUING, "Completed outbox event processing", null))
                    .subscribe();

        } catch (Exception e) {
            Helpers.log(LocalDateTime.now().toString(), LogLevelEnum.error, OperationNameEnum.OUTBOX_EVENTS_SCHEDUING, "Error processing outbox events", new RuntimeException(e));
        }

    }


    private Mono<RepaymentOutboxEvent> markAsPublished(String refID, RepaymentOutboxEvent e) {
        Helpers.log(refID, LogLevelEnum.info, OperationNameEnum.PUBLISH_OUTBOX_EVENTS_TASK, "marking outbox event as published", null);
        e.setPublished(true);
        return outboxEventRepository.save(e)
                .doOnError(throwable -> Helpers.log("PUBLISH_OUT_BOX_EVENT", LogLevelEnum.ERROR, OperationNameEnum.PUBLISH_OUTBOX_EVENTS_TASK, "Error occurred saving outbox event", new RuntimeException(throwable)))
                .doOnSuccess(res -> Helpers.log(refID, LogLevelEnum.info, OperationNameEnum.PUBLISH_OUTBOX_EVENTS_TASK, "Saved outbox event: " + res.getId(), null));
    }

    @Override
    public void delete() {
    }
}
