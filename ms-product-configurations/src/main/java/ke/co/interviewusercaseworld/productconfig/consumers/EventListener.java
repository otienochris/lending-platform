package ke.co.interviewusercaseworld.productconfig.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.commands.ProductValidationCommand;
import ke.co.interviewusercaseworld.commons.dto.events.ProductValidationEvent;
import ke.co.interviewusercaseworld.commons.dto.responses.LoanProductResponseDto;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.productconfig.model.entity.OutboxEvent;
import ke.co.interviewusercaseworld.productconfig.repository.OutBoxEventRepository;
import ke.co.interviewusercaseworld.productconfig.service.LoanProductService;
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
public class EventListener {

    private final LoanProductService loanProductService;
    private final ObjectMapper objectMapper;
    private final OutBoxEventRepository outBoxEventRepository;

    @KafkaListener(topics = "product.validation.command", groupId = "product-configurations")
    public Mono<Void> onProductValidationCommand(String payload) {
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from product.validation.command: " + payload, null);

        ProductValidationCommand productValidationCommand = null;
        try{
            productValidationCommand = objectMapper.readValue(payload, ProductValidationCommand.class);
            Helpers.log("product.validation.command", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Parsed product.validation.command", null);
        } catch (Exception e){
            Helpers.log("product.validation.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing product.validation.command", e);
            return Mono.empty();
        }

        ProductValidationCommand finalProductValidationCommand = productValidationCommand;
        UUID productId = productValidationCommand.getProductId();
        if (productId == null ) {
            Helpers.log("product.validation.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Product id is null", null);
            return Mono.empty();
        }
        return loanProductService.getLoanProduct(productId, Map.of())
                .flatMap(res -> {

                    ProductValidationEvent productValidationEvent = ProductValidationEvent.builder()
                            .productId(finalProductValidationCommand.getProductId())
                            .commandId(finalProductValidationCommand.getCommandId())
                            .message(res.getHeader().getCustomerMessage())
                            .status(res.getHeader().getResponseCode().name())
                            .build();

                    if (res.getHeader().getResponseCode().name().startsWith("RC_2")) {
                        LoanProductResponseDto body = res.getBody();
                        Boolean supportInstallments = body.getSupportInstallments();
                        BigDecimal minAmount = body.getMinAmount();
                        BigDecimal maxAmount = body.getMaxAmount();

                        Boolean installment = finalProductValidationCommand.getInstallment();
                        BigDecimal loanAmount = finalProductValidationCommand.getLoanAmount();

                        if (installment && !supportInstallments) {
                            productValidationEvent.setStatus(ResponseCodes.RC_400.name());
                            productValidationEvent.setMessage("Installment is not supported for this loan amount");
                        } else if (loanAmount.compareTo(minAmount) < 0 || loanAmount.compareTo(maxAmount) > 0) {
                            productValidationEvent.setStatus(ResponseCodes.RC_400.name());
                            productValidationEvent.setMessage("Loan amount must be between " + minAmount + " and " + maxAmount);
                        }

                        productValidationEvent.setProductDetails(res.getBody());

                    }

                    try {
                        String payloadString = objectMapper.writeValueAsString(productValidationEvent);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .eventType(CommandsEnum.PRODUCT_VALIDATION_EVENT.name())
                                .aggregateType("LOAN")
                                .aggregateId(finalProductValidationCommand.getCommandId().toString())
                                .createdAt(LocalDateTime.now())
                                .isPublished(false)
                                .payload(payloadString)
                                .build();
                        return outBoxEventRepository.save(outboxEvent)
                                .doOnError(throwable -> Helpers.log("product.validation.productValidationEvent", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error occured saving outbox event", new RuntimeException(throwable)))
                                .doOnSuccess(res1 -> Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Saved product.validation.event", null));
                    } catch (Exception e) {
                        Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error saving product.validation.event", e);
                        return Mono.empty();
                    }
                }).then();
    }
}
