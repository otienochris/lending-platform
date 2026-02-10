package ke.co.interviewusercaseworld.repayment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.commands.RepaymentCommand;
import ke.co.interviewusercaseworld.commons.dto.events.DisbursementEvent;
import ke.co.interviewusercaseworld.commons.dto.events.RepaymentEvent;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.entities.OutboxEvent;
import ke.co.interviewusercaseworld.repayment.repository.OutBoxEventRepository;
import ke.co.interviewusercaseworld.repayment.services.LoanRepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@Component
@RequiredArgsConstructor
public class EventListeners {

    private final LoanRepaymentService loanRepaymentService;
    private final ObjectMapper objectMapper;
    private final OutBoxEventRepository outBoxEventRepository;

    @KafkaListener(topics = "loan.repayment.command", groupId = "loan-repayment")
    public Mono<Void> onProductValidationCommand(String payload) {
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from loan.repayment.command: " + payload, null);

        RepaymentCommand repaymentCommand = null;
        try{
            repaymentCommand = objectMapper.readValue(payload, RepaymentCommand.class);
            Helpers.log("loan.repayment.command", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Parsed loan.repayment.command", null);
        } catch (Exception e){
            Helpers.log("loan.repayment.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing loan.repayment.command", e);
            return Mono.empty();
        }

        if (repaymentCommand.getPrincipal() == null) {
            Helpers.log("loan.repayment.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Principal amount is null", null);
            return Mono.empty();
        }

        RepaymentCommand finalProductValidationCommand = repaymentCommand;
        UUID productId = repaymentCommand.getProductId();

        if (productId == null ) {
            Helpers.log("loan.repayment.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Product id is null", null);
            return Mono.empty();
        }
        BigDecimal amount = repaymentCommand.getPrincipal();
        GenericRequest<DefaultResponseHeader, LoanRepaymentSchedulingDto> disbursementRequest = GenericRequest.<DefaultResponseHeader, LoanRepaymentSchedulingDto>builder()
                .header(DefaultResponseHeader.builder().build())
                .body(LoanRepaymentSchedulingDto.builder()
                        .loanId(repaymentCommand.getCommandId())
                        .productId(productId)
                        .principal(amount)
                        .customerId(repaymentCommand.getCustomerId())
                        .interestRate(repaymentCommand.getInterestRate())
                        .tenure(repaymentCommand.getTenure())
                        .interestRateType(repaymentCommand.getInterestRateType())
                        .tenureType(repaymentCommand.getTenureType())
                        .isInstallment(repaymentCommand.getIsInstallment())
                        .build())
                .build();
        return loanRepaymentService.schedule(disbursementRequest)
                .doOnError(throwable -> Helpers.log("loan.repayment.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error occurred scheduling loan repayment", new RuntimeException(throwable)))
                .flatMap(res -> {
                    RepaymentEvent command = RepaymentEvent.builder()
                            .commandId(finalProductValidationCommand.getCommandId())
                            .message(res.getHeader().getCustomerMessage())
                            .status(res.getHeader().getResponseCode().name())
                            .dueDate(now().plusMonths(1))
                            .totalLoanAmount(res.getBody() == null? BigDecimal.ZERO: res.getBody().getTotalOutstandingAmount())// todo
                            .totalInterest(res.getBody() == null? BigDecimal.ZERO: res.getBody().getTotalInterest())
                            .build();

                    try {
                        String payloadString = objectMapper.writeValueAsString(command);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .eventType(CommandsEnum.REPAYMENT_EVENT.name())
                                .aggregateType("LOAN")
                                .aggregateId(finalProductValidationCommand.getCommandId().toString())
                                .createdAt(now())
                                .isPublished(false)
                                .payload(payloadString)
                                .build();
                        return outBoxEventRepository.save(outboxEvent)
                                .doOnError(throwable -> Helpers.log("loan.disbursement.event", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error occured saving outbox event", new RuntimeException(throwable)))
                                .doOnSuccess(res1 -> Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Saved loan.repayment.event", null));
                    } catch (Exception e) {
                        Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error saving product.validation.event", e);
                        return Mono.empty();
                    }
                }).then();
    }
}
