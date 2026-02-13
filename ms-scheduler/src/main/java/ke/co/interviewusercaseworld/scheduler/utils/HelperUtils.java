package ke.co.interviewusercaseworld.scheduler.utils;

import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;

import java.util.Map;

public class HelperUtils {
    public static Mono<Boolean> processEvent(String refId, String eventType, String aggregateId, String payload, Map<CommandsEnum, String> commandTopicMap, KafkaTemplate<String, String> kafka) {
        Helpers.log(refId, LogLevelEnum.info, OperationNameEnum.UPDATE_OUTBOX_EVENT_STATUS, "Processing event ID: " + eventType, null);
        CommandsEnum commandsEnum;
        try {
            commandsEnum = CommandsEnum.valueOf(eventType);
        } catch (Exception e) {
            String message = "Invalid event type: " + eventType;
            Helpers.log(refId, LogLevelEnum.warn, OperationNameEnum.UPDATE_OUTBOX_EVENT_STATUS, message, null);
            return Mono.error(new RuntimeException(message));
        }

        String topic = commandTopicMap.getOrDefault(commandsEnum, "");
        if (topic.isEmpty()) {
            String message = "No topic found for command: " + commandsEnum;
            Helpers.log(refId, LogLevelEnum.error, OperationNameEnum.UPDATE_OUTBOX_EVENT_STATUS, message, null);
            return Mono.error(new RuntimeException(message));
        }

        Helpers.log(refId, LogLevelEnum.info, OperationNameEnum.UPDATE_OUTBOX_EVENT_STATUS, "Publishing event ID: " + eventType + " to topic: " + topic, null);

        return Mono.fromFuture(kafka.send(topic, aggregateId, payload))
                .then(Mono.defer(() -> Mono.just(true)))
                .doOnSuccess(v -> Helpers.log(refId, LogLevelEnum.info, OperationNameEnum.UPDATE_OUTBOX_EVENT_STATUS, "Successfully processed event ID: " + eventType, null));
    }
}
