package ke.co.interviewusercaseworld.disbursement.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.commands.DisbursementCommand;
import ke.co.interviewusercaseworld.commons.dto.events.DisbursementEvent;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.disbursement.model.dto.requests.DisbursementRequest;
import ke.co.interviewusercaseworld.disbursement.model.entities.OutboxEvent;
import ke.co.interviewusercaseworld.disbursement.repository.OutBoxEventRepository;
import ke.co.interviewusercaseworld.disbursement.service.LoanDisbursementService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventListeners {

    private final LoanDisbursementService loanDisbursementService;
    private final ObjectMapper objectMapper;
    private final OutBoxEventRepository outBoxEventRepository;

    @KafkaListener(topics = "loan.disbursement.command", groupId = "loan-disbursement")
    public Mono<Void> onProductValidationCommand(String payload) {
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from loan.disbursement.command: " + payload, null);

        DisbursementCommand productValidationCommand = null;
        try{
            productValidationCommand = objectMapper.readValue(payload, DisbursementCommand.class);
            Helpers.log("loan.disbursement.command", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Parsed loan.disbursement.command", null);
        } catch (Exception e){
            Helpers.log("loan.disbursement.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing loan.disbursement.command", e);
            return Mono.empty();
        }

        DisbursementCommand finalProductValidationCommand = productValidationCommand;
        UUID productId = productValidationCommand.getProductId();

        if (productId == null ) {
            Helpers.log("loan.disbursement.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Product id is null", null);
            return Mono.empty();
        }
        BigDecimal amount = productValidationCommand.getLoanAmount();
        GenericRequest<DefaultResponseHeader, DisbursementRequest> disbursementRequest = GenericRequest.<DefaultResponseHeader, DisbursementRequest>builder()
                .header(DefaultResponseHeader.builder().build())
                .body(DisbursementRequest.builder()
                        .loanId(productValidationCommand.getCommandId())
                        .productId(productId)
                        .amount(amount)
                        .customerId(productValidationCommand.getCustomerId())
                        .destinationWallet(finalProductValidationCommand.getDestinationWallet())
                        .walletId(finalProductValidationCommand.getWalletId())
                        .build())
                .build();
        return loanDisbursementService.disburse(disbursementRequest)
                .flatMap(res -> {
                    DisbursementEvent command = DisbursementEvent.builder()
                            .commandId(finalProductValidationCommand.getCommandId())
                            .message(res.getHeader().getCustomerMessage())
                            .status(res.getHeader().getResponseCode().name())
                            .build();
                    try {
                        String payloadString = objectMapper.writeValueAsString(command);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .eventType(CommandsEnum.DISBURSE_EVENT.name())
                                .aggregateType("LOAN")
                                .aggregateId(finalProductValidationCommand.getCommandId().toString())
                                .createdAt(LocalDateTime.now())
                                .isPublished(false)
                                .payload(payloadString)
                                .build();
                        return outBoxEventRepository.save(outboxEvent)
                                .doOnError(throwable -> Helpers.log("loan.disbursement.event", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error occured saving outbox event", new RuntimeException(throwable)))
                                .doOnSuccess(res1 -> Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Saved loan.disbursement.event", null));
                    } catch (Exception e) {
                        Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error saving product.validation.event", e);
                        return Mono.empty();
                    }
                }).then();
    }
}
