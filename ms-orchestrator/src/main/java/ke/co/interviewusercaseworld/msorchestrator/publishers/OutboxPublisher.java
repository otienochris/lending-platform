package ke.co.interviewusercaseworld.msorchestrator.publishers;

import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.msorchestrator.configs.OrchestratorConfigsProperties;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.OutBoxEvent;
import ke.co.interviewusercaseworld.msorchestrator.repository.OutBoxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutBoxEventRepository outboxRepo;
    private final KafkaTemplate<String, String> kafka;
    private final OrchestratorConfigsProperties appProperties;


    @Scheduled(fixedDelay = 30000)
    public void publishOutboxEvent(){
        Helpers.log("PUBLISH_OUT_BOX_EVENT", LogLevelEnum.info, OperationNameEnum.PUBLISH_OUTBOX_EVENTS_TASK, "publishing outbox events", null);

        outboxRepo.findTop20ByIsPublishedFalse()
                .flatMap(outBoxEvent -> {
                    CommandsEnum commandsEnum = CommandsEnum.valueOf(outBoxEvent.getEventType());
                    String topic = appProperties.getServiceProperties().getOrchestratorProperties().getKafkaConfigs().getTopicsForCommand().getOrDefault(commandsEnum, "");
                    Helpers.log("PUBLISH_OUT_BOX_EVENT", LogLevelEnum.info, OperationNameEnum.PUBLISH_OUTBOX_EVENTS_TASK, "publishing outbox events to topic: " + topic, null);
                    if (!topic.isEmpty()) {
                        return Mono.fromFuture(kafka.send(topic, outBoxEvent.getAggregateId(), outBoxEvent.getPayload()))
                                .then(markAsPublished(outBoxEvent));
                    }
                    return Mono.empty();
                }).subscribe();

    }

    private Mono<Void> markAsPublished(OutBoxEvent e) {
        Helpers.log("PUBLISH_OUT_BOX_EVENT", LogLevelEnum.info, OperationNameEnum.PUBLISH_OUTBOX_EVENTS_TASK, "marking outbox event as published", null);
        e.setPublished(true);
        return outboxRepo.save(e).then();
    }
}
