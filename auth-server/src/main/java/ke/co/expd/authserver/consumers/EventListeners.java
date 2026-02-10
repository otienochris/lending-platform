package ke.co.expd.authserver.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.expd.authserver.model.entities.OutboxEvent;
import ke.co.expd.authserver.repoisitory.OutBoxEventRepository;
import ke.co.expd.authserver.service.AuthService;
import ke.co.interviewusercaseworld.commons.dto.commands.UserValidationCommand;
import ke.co.interviewusercaseworld.commons.dto.events.ProductValidationEvent;
import ke.co.interviewusercaseworld.commons.dto.events.UserValidationEvent;
import ke.co.interviewusercaseworld.commons.dto.responses.UserValidationResponse;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventListeners {

    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final OutBoxEventRepository outBoxEventRepository;

    @KafkaListener(topics = "user.validation.command", groupId = "auth-server")
    public Mono<Void> onProductValidationCommand(String payload) {
        Helpers.log("user.validation.command", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from user.validation.command: " + payload, null);

        UserValidationCommand userValidationCommand;
        try{
            userValidationCommand = objectMapper.readValue(payload, UserValidationCommand.class);
            Helpers.log("user.validation.command", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Parsed user.validation.command", null);
        } catch (Exception e){
            Helpers.log("user.validation.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing user.validation.command", e);
            return Mono.empty();
        }

        UUID userId = userValidationCommand.getUserId();
        UserValidationCommand finalUserValidationCommand = userValidationCommand;
        return authService.validateUser(userId, Map.of())
                .flatMap(res -> {
                    Helpers.log("user.validation.command", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Validated user: " + userId, null);

                    UserValidationEvent command = UserValidationEvent.builder()
                            .userId(finalUserValidationCommand.getUserId())
                            .commandId(finalUserValidationCommand.getCommandId())
                            .message(res.getHeader().getCustomerMessage())
                            .status(res.getHeader().getResponseCode().name())
                            .build();

                    if (ResponseCodes.RC_200.name().equalsIgnoreCase(res.getHeader().getResponseCode().name())) {
                        UserValidationResponse body = res.getBody();
                        BigDecimal loanLimit = body.getLoanLimit();

                        if (loanLimit == null || finalUserValidationCommand.getLoanAmount().compareTo(loanLimit) > 0) {
                            command.setStatus(ResponseCodes.RC_400.name());
                            command.setMessage("Loan amount exceeds loan limit");
                        }
                    }

                    try {
                        String payloadString = objectMapper.writeValueAsString(command);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .eventType(CommandsEnum.USER_VALIDATION_EVENT.name())
                                .aggregateType("LOAN")
                                .aggregateId(finalUserValidationCommand.getCommandId().toString())
                                .createdAt(LocalDateTime.now())
                                .isPublished(false)
                                .payload(payloadString)
                                .build();
                        return outBoxEventRepository.save(outboxEvent)
                                .doOnError(throwable -> Helpers.log("user.validation.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error occurred saving outbox event", new RuntimeException(throwable)))
                                .doOnSuccess(res1 -> Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Saved user.validation.event", null));
                    } catch (Exception e) {
                        Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error saving user.validation.event", e);
                        return Mono.empty();
                    }
                }).then();


    }
}
