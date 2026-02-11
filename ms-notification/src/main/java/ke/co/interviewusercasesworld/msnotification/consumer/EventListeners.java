package ke.co.interviewusercasesworld.msnotification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercasesworld.msnotification.model.entity.OutboxEvent;
import ke.co.interviewusercasesworld.msnotification.repository.OutBoxEventRepository;
import ke.co.interviewusercasesworld.msnotification.service.NotificationService;
import ke.co.interviewusercaseworld.commons.dto.commands.NotificationCommand;
import ke.co.interviewusercaseworld.commons.dto.events.NotificationEvent;
import ke.co.interviewusercaseworld.commons.dto.events.RepaymentEvent;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static java.time.LocalDateTime.now;

@Component
@RequiredArgsConstructor
public class EventListeners {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final OutBoxEventRepository outBoxEventRepository;

    @KafkaListener(topics = "notification.command", groupId = "notifications")
    public Mono<Void> onRepaymentPrevalidationCommand(String payload) {

        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from notification.command: " + payload, null);
        NotificationCommand notificationCommand = null;
        try {
            notificationCommand = objectMapper.readValue(payload, NotificationCommand.class);
            Helpers.log("notification.command", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Parsed notification.command", null);
        } catch (Exception e) {
            Helpers.log("notification.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing notification.command", e);
            return Mono.empty();
        }

        if (notificationCommand.getCommandId() == null) {
            Helpers.log("notification.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Command id is null", null);
            return Mono.empty();
        }

        NotificationCommand finalRepaymentCommand = notificationCommand;

        return notificationService.schedule(notificationCommand)
                .doOnError(throwable -> Helpers.log("notification.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error occurred scheduling loan repayment", new RuntimeException(throwable)))
                .flatMap(res -> {
                    NotificationEvent command = NotificationEvent.builder()
                            .commandId(finalRepaymentCommand.getCommandId())
                            .message(res.getHeader().getCustomerMessage())
                            .status(res.getHeader().getResponseCode().name())
                            .build();

                    try {
                        String payloadString = objectMapper.writeValueAsString(command);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .eventType(CommandsEnum.NOTIFY_EVENT.name())
                                .aggregateType("NOTIFICATIONS")
                                .aggregateId(finalRepaymentCommand.getCommandId().toString())
                                .createdAt(now())
                                .isPublished(false)
                                .payload(payloadString)
                                .build();
                        return outBoxEventRepository.save(outboxEvent)
                                .doOnError(throwable -> Helpers.log("notification.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error occurred saving outbox event", new RuntimeException(throwable)))
                                .doOnSuccess(res1 -> Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Saved notification.event", null));
                    } catch (Exception e) {
                        Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error saving notification.command", e);
                        return Mono.empty();
                    }
                }).then();
    }

}
