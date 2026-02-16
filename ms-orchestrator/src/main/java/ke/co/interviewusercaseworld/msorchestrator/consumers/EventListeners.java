package ke.co.interviewusercaseworld.msorchestrator.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.commands.DisbursementCommand;
import ke.co.interviewusercaseworld.commons.dto.commands.NotificationCommand;
import ke.co.interviewusercaseworld.commons.dto.commands.RepaymentCommand;
import ke.co.interviewusercaseworld.commons.dto.commands.RepaymentSchedulingCommand;
import ke.co.interviewusercaseworld.commons.dto.events.*;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.ProductValidationOutcomeDto;
import ke.co.interviewusercaseworld.commons.enums.*;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanRepaymentRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.commands.UserValidationCommand;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.OutBoxEvent;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.Saga;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.SagaStep;
import ke.co.interviewusercaseworld.msorchestrator.repository.OutBoxEventRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaStepRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static ke.co.interviewusercaseworld.commons.enums.NotificationTemplateEnum.*;
import static ke.co.interviewusercaseworld.msorchestrator.service.impl.LoanServiceImpl.LOAN_VALIDATION_STEP;

@Component
@RequiredArgsConstructor
public class EventListeners {

    public static final String USER_VALIDATION_STEP = "USER_VALIDATION";
    public static final String PRODUCT_VALIDATION_STEP = "PRODUCT_VALIDATION";
    public static final String LOAN_AGGREGATE = "LOAN";
    public static final String LOAN_DISBURSEMENT_STEP = "LOAN_DISBURSEMENT";
    public static final String COMPLETED_STATUS = "COMPLETED";
    public static final String LOAN_REPAYMENT_SCHEDULING_STEP = "LOAN_REPAYMENT_SCHEDULING";
    public static final String NOTIFY_CUSTOMER_STEP = "NOTIFY_CUSTOMER";
    public static final String LOAN_REPAYMENT_STEP = "LOAN_REPAYMENT";
    private final ObjectMapper objectMapper;
    private final SagaRepository sagaRepo;
    private final TransactionalOperator tx;
    private final SagaStepRepository stepRepo;
    private final OutBoxEventRepository outboxRepo;

    @KafkaListener(topics = {"product.validation.event"}, groupId = "orchestrator")
    public Mono<Void> onProductValidationEvent(String payload) {


        ProductValidationEvent productValidationEvent;
        try {
            productValidationEvent = objectMapper.readValue(payload, ProductValidationEvent.class);
            Helpers.log(productValidationEvent.getCommandId().toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Parsed product.validation.event", null);
        } catch (Exception e) {
            Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing product.validation.event: " + payload, e);
            return Mono.empty();
        }

        UUID loanId = productValidationEvent.getCommandId();
        Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from product.validation.event", null);

        return sagaRepo.findByBusinessKey(loanId)
                .flatMap(saga -> stepRepo.findBySagaIdAndStepName(saga.getId(), PRODUCT_VALIDATION_STEP)
                        .collectList()
                        .flatMap(sagaStep -> tx.execute(status -> {

                            GenericRequest<DefaultRequestHeader, LoanApplicationRequest> originalRequest;
                            String payloadString;
                            String notificationCommandString;
                            try {

                                String sagaOriginalRequest = saga.getOriginalRequest();

                                originalRequest = objectMapper.readValue(sagaOriginalRequest, new TypeReference<GenericRequest<DefaultRequestHeader, LoanApplicationRequest>>() {
                                });

                                UserValidationCommand command = UserValidationCommand.builder()
                                        .userId(originalRequest.getBody().getCustomerId())
                                        .commandId(loanId)
                                        .loanAmount(originalRequest.getBody().getLoanAmount())
                                        .build();
                                String message = productValidationEvent.getMessage();
                                NotificationCommand notificationCommand = NotificationCommand.builder()
                                        .commandId(loanId)
                                        .types(List.of(NotificationTypeEnum.SMS, NotificationTypeEnum.EMAIL))
                                        .template(PRODUCT_VALIDATION_FAILED_TEMPLATE)
                                        .recipient(NotificationCommand.Recipient.builder().msisdn("254742887480").to(List.of("abc@xyz.com")).build())
                                        .templateParamValues(Map.of("CUSTOMER_MESSAGE", message == null ? "Product validation failed" : message))
                                        .build();

                                payloadString = objectMapper.writeValueAsString(command);
                                notificationCommandString = objectMapper.writeValueAsString(notificationCommand);
                            } catch (Exception e) {
                                Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error creating user validation command", e);
                                throw new RuntimeException(e);
                            }

                            saga.setUpdatedAt(now());


                            OutBoxEvent outBoxEvent;
                            String nextStep;
                            if (!ResponseCodes.RC_200.name().equalsIgnoreCase(productValidationEvent.getStatus())) {
                                nextStep = NOTIFY_CUSTOMER_STEP;
                                saga.setCurrentStep(nextStep);
                                Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Sending notification event", null);
                                outBoxEvent = OutBoxEvent.builder()
                                        .aggregateType(LOAN_AGGREGATE)
                                        .aggregateId(loanId.toString())
                                        .createdAt(now())
                                        .isPublished(false)
                                        .payload(notificationCommandString)
                                        .eventType(CommandsEnum.NOTIFY_COMMAND.name())
                                        .build();
                            } else {
                                nextStep = USER_VALIDATION_STEP;
                                saga.setCurrentStep(nextStep);
                                Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Sending user validation event", null);
                                outBoxEvent = OutBoxEvent.builder()
                                        .aggregateType(LOAN_AGGREGATE)
                                        .aggregateId(loanId.toString())
                                        .createdAt(now())
                                        .isPublished(false)
                                        .payload(payloadString)
                                        .eventType(CommandsEnum.USER_VALIDATION_COMMAND.name())
                                        .build();
                            }
                            return updateSagaStepStatus(payload, sagaStep)
                                    .flatMap(step -> {
                                        return stepRepo.save(step)
                                                .then(stepRepo.save(nextStep(saga)))
                                                .then(outboxRepo.save(outBoxEvent))
                                                .then(sagaRepo.save(saga))
                                                .doOnError(throwable -> Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error saving saga", new RuntimeException(throwable)));
                                    });
                        }).then())
                );
    }

    @KafkaListener(topics = {"user.validation.event"}, groupId = "orchestrator")
    public Mono<Void> onUserValidationEvent(String payload) {

        UserValidationEvent userValidationEvent;

        try {
            userValidationEvent = objectMapper.readValue(payload, UserValidationEvent.class);
            Helpers.log(userValidationEvent.getCommandId().toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Parsed user.validation.event", null);
        } catch (Exception e) {
            Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing user.validation.event", e);
            return Mono.empty();
        }

        UUID loanId = userValidationEvent.getCommandId();
        Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from user.validation.event", null);


        return sagaRepo.findByBusinessKey(loanId)
                .flatMap(saga -> stepRepo.findBySagaIdAndStepName(saga.getId(), USER_VALIDATION_STEP)
                        .collectList()
                        .flatMap(sagaStep -> tx.execute(status -> {

                            GenericRequest<DefaultRequestHeader, LoanApplicationRequest> originalRequest;
                            String payloadString;

                            boolean notSuccessful = !ResponseCodes.RC_200.name().equalsIgnoreCase(userValidationEvent.getStatus());
                            try {
                                originalRequest = objectMapper.readValue(saga.getOriginalRequest(), new TypeReference<GenericRequest<DefaultRequestHeader, LoanApplicationRequest>>() {
                                });


                                if (notSuccessful) {
                                    NotificationCommand notificationCommand = NotificationCommand.builder()
                                            .commandId(loanId)
                                            .types(List.of(NotificationTypeEnum.SMS, NotificationTypeEnum.EMAIL))
                                            .template(USER_VALIDATION_FAILED_TEMPLATE)
                                            .recipient(NotificationCommand.Recipient.builder().msisdn("254742887480").to(List.of("abc@xyz.com")).build())
                                            .templateParamValues(Map.of("CUSTOMER_MESSAGE", userValidationEvent.getMessage()))
                                            .build();
                                    payloadString = objectMapper.writeValueAsString(notificationCommand);
                                } else {
                                    DisbursementCommand command = DisbursementCommand.builder()
                                            .destinationWallet(originalRequest.getBody().getWalletType())
                                            .customerId(originalRequest.getBody().getCustomerId())
                                            .productId(originalRequest.getBody().getProductId())
                                            .loanAmount(originalRequest.getBody().getLoanAmount())
                                            .loanPurpose(originalRequest.getBody().getLoanPurpose())
                                            .commandId(loanId)
                                            .walletId(originalRequest.getBody().getWalletId())
                                            .destinationWallet(originalRequest.getBody().getWalletType())
                                            .build();
                                    payloadString = objectMapper.writeValueAsString(command);
                                }

                            } catch (Exception e) {
                                Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error creating loan application command", e);
                                throw new RuntimeException(e);
                            }


                            saga.setUpdatedAt(now());


                            OutBoxEvent outBoxEvent;
                            String nextStep;
                            if (notSuccessful) {
                                nextStep = NOTIFY_CUSTOMER_STEP;
                                saga.setCurrentStep(nextStep);
                                outBoxEvent = OutBoxEvent.builder()
                                        .aggregateType(LOAN_AGGREGATE)
                                        .aggregateId(loanId.toString())
                                        .createdAt(now())
                                        .isPublished(false)
                                        .payload(payloadString)
                                        .eventType(CommandsEnum.NOTIFY_COMMAND.name())
                                        .build();
                            } else {
                                nextStep = LOAN_DISBURSEMENT_STEP;
                                saga.setCurrentStep(nextStep);
                                outBoxEvent = OutBoxEvent.builder()
                                        .aggregateType(LOAN_AGGREGATE)
                                        .aggregateId(loanId.toString())
                                        .createdAt(now())
                                        .isPublished(false)
                                        .payload(payloadString)
                                        .eventType(CommandsEnum.DISBURSE_COMMAND.name())
                                        .build();
                            }


                            return updateSagaStepStatus(payload, sagaStep)
                                    .flatMap(step -> {
                                        return stepRepo.save(step)
                                                .then(stepRepo.save(nextStep(saga)))
                                                .then(outboxRepo.save(outBoxEvent))
                                                .then(sagaRepo.save(saga));
                                    });


                        }).then()));

    }

    @KafkaListener(topics = {"loan.disbursement.event"}, groupId = "orchestrator")
    public Mono<Void> onDisbursementEvent(String payload) {

        DisbursementEvent disbursementEvent;
        try {
            disbursementEvent = objectMapper.readValue(payload, DisbursementEvent.class);
        } catch (Exception e) {
            Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing loan.disbursement.event", e);
            return Mono.empty();
        }

        UUID loanId = disbursementEvent.getCommandId();
        Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from loan.disbursement.event", null);

        boolean notSuccessful = !ResponseCodes.RC_200.name().equalsIgnoreCase(disbursementEvent.getStatus());

        return sagaRepo.findByBusinessKey(loanId)
                .flatMap(saga -> {
                    Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Found saga for loanId: " + loanId, null);
                    return getProductDetails(notSuccessful, saga.getId(), PRODUCT_VALIDATION_STEP) // is successful, get product details from previous step
                            .defaultIfEmpty(ProductValidationOutcomeDto.builder().build())
                            .flatMap(loanProductResponseDto -> {
                                return stepRepo.findBySagaIdAndStepName(saga.getId(), LOAN_DISBURSEMENT_STEP)
                                        .collectList()
                                        .flatMap(sagaStep -> tx.execute(status -> {

                                            GenericRequest<DefaultRequestHeader, LoanApplicationRequest> originalRequest;
                                            String payloadString;
                                            try {
                                                originalRequest = objectMapper.readValue(saga.getOriginalRequest(), new TypeReference<GenericRequest<DefaultRequestHeader, LoanApplicationRequest>>() {
                                                });


                                                if (notSuccessful) {
                                                    String message = disbursementEvent.getMessage();
                                                    NotificationCommand notificationCommand = NotificationCommand.builder()
                                                            .commandId(loanId)
                                                            .types(List.of(NotificationTypeEnum.SMS, NotificationTypeEnum.EMAIL))
                                                            .template(DISBURSAL_FAILED_TEMPLATE)
                                                            .recipient(NotificationCommand.Recipient.builder().msisdn("254742887480").to(List.of("abc@xyz.com")).build())
                                                            .templateParamValues(Map.of("CUSTOMER_MESSAGE", message == null ? "Disbursement failed" : message))
                                                            .build();
                                                    payloadString = objectMapper.writeValueAsString(notificationCommand);
                                                } else {
                                                    RepaymentSchedulingCommand command = RepaymentSchedulingCommand.builder()
                                                            .loanId(loanId)
                                                            .commandId(loanId)
                                                            .principal(originalRequest.getBody().getLoanAmount())
                                                            .productId(originalRequest.getBody().getProductId())
                                                            .tenure(originalRequest.getBody().getTenure())
                                                            .customerId(originalRequest.getBody().getCustomerId())
                                                            .repaymentOption(originalRequest.getBody().getRepaymentOption())
                                                            .interestRate(loanProductResponseDto.getProductDetails().getInterestRate())
                                                            .interestRateType(loanProductResponseDto.getProductDetails().getInterestRateType())
                                                            .tenureType(loanProductResponseDto.getProductDetails().getTenureOptionsType())
                                                            .installmentFrequency(originalRequest.getBody().getInstallmentFrequency())
                                                            .build();

                                                    payloadString = objectMapper.writeValueAsString(command);
                                                }

                                            } catch (Exception e) {
                                                Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error creating loan repayment command", e);
                                                throw new RuntimeException(e);
                                            }


                                            saga.setUpdatedAt(now());


                                            OutBoxEvent outBoxEvent;
                                            String nextStep;
                                            if (notSuccessful) {
                                                nextStep = NOTIFY_CUSTOMER_STEP;
                                                outBoxEvent = OutBoxEvent.builder()
                                                        .aggregateType(LOAN_AGGREGATE)
                                                        .aggregateId(loanId.toString())
                                                        .createdAt(now())
                                                        .isPublished(false)
                                                        .payload(payloadString)
                                                        .eventType(CommandsEnum.NOTIFY_COMMAND.name())
                                                        .build();
                                            } else {
                                                nextStep = LOAN_REPAYMENT_SCHEDULING_STEP;
                                                System.out.println(payloadString);
                                                outBoxEvent = OutBoxEvent.builder()
                                                        .aggregateType(LOAN_AGGREGATE)
                                                        .aggregateId(loanId.toString())
                                                        .createdAt(now())
                                                        .isPublished(false)
                                                        .payload(payloadString)
                                                        .eventType(CommandsEnum.REPAYMENT_SCHEDULING_COMMAND.name())
                                                        .build();
                                            }

                                            saga.setCurrentStep(nextStep);

                                            return updateSagaStepStatus(payload, sagaStep)
                                                    .flatMap(step -> {
                                                        return stepRepo.save(step)
                                                                .then(stepRepo.save(nextStep(saga)))
                                                                .then(outboxRepo.save(outBoxEvent))
                                                                .then(sagaRepo.save(saga));
                                                    });

                                        }).then());
                            });
                });


    }

    private Mono<ProductValidationOutcomeDto> getProductDetails(boolean notSuccessful, UUID sagaId, String stepName) {

        return notSuccessful ? Mono.empty() :
                stepRepo.findBySagaIdAndStepName(sagaId, stepName)
                        .collectList()
                        .doOnSuccess(sagaStep -> Helpers.log(sagaId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Found step: " + stepName, null))
                        .map(it -> {
                            try {
                                SagaStep sagaStep = it.get(0);
                                //System.out.println("outcome: "  + it.getOutcome());
                                ProductValidationOutcomeDto productValidationOutcomeDto = objectMapper.readValue(sagaStep.getOutcome(), ProductValidationOutcomeDto.class);
                                if (productValidationOutcomeDto.getProductDetails() == null) {
                                    throw new RuntimeException("Error getting product details from " + stepName + ". Product details not found in outcome: " + sagaStep.getOutcome());
                                }
                                return productValidationOutcomeDto;
                            } catch (Exception e) {
                                Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error getting outcome details from " + stepName, e);
                                throw new RuntimeException(e);
                            }

                        });
    }

    @KafkaListener(topics = {"loan.repayment.scheduling.event"}, groupId = "orchestrator")
    public Mono<Void> onRepaymentSchedulingEvent(String payload) {

        RepaymentSchedulingEvent repaymentSchedulingEvent;
        try {
            repaymentSchedulingEvent = objectMapper.readValue(payload, RepaymentSchedulingEvent.class);
        } catch (Exception e) {
            Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing loan.repayment.scheduling.event", e);
            return Mono.empty();
        }

        UUID loanId = repaymentSchedulingEvent.getCommandId();
        Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from loan.repayment.scheduling.event: " + payload, null);

        RepaymentSchedulingEvent finalRepaymentSchedulingEvent = repaymentSchedulingEvent;
        boolean notSuccessful = !ResponseCodes.RC_200.name().equalsIgnoreCase(finalRepaymentSchedulingEvent.getStatus());
        return sagaRepo.findByBusinessKey(loanId)
                .flatMap(saga -> stepRepo.findBySagaIdAndStepName(saga.getId(), LOAN_REPAYMENT_SCHEDULING_STEP)
                        .collectList()
                        .flatMap(sagaStep -> tx.execute(status -> {

                            saga.setCurrentStep(NOTIFY_CUSTOMER_STEP);
                            saga.setUpdatedAt(now());


                            String payloadString;
                            String nextStep;
                            try {
                                if (notSuccessful) {
                                    nextStep = NOTIFY_CUSTOMER_STEP;
                                    String message = finalRepaymentSchedulingEvent.getMessage();
                                    NotificationCommand notificationCommand = NotificationCommand.builder()
                                            .commandId(loanId)
                                            .types(List.of(NotificationTypeEnum.SMS, NotificationTypeEnum.EMAIL))
                                            .template(REPAYMENT_FAILED_TEMPLATE)
                                            .recipient(NotificationCommand.Recipient.builder().msisdn("254742887480").to(List.of("abc@xyz.com")).build())
                                            .templateParamValues(Map.of("CUSTOMER_MESSAGE", message == null ? "Disbursement failed" : message))
                                            .build();
                                    payloadString = objectMapper.writeValueAsString(notificationCommand);
                                } else {
                                    nextStep = NOTIFY_CUSTOMER_STEP;
                                    NotificationCommand command = NotificationCommand.builder()
                                            .commandId(loanId)
                                            .template(SUCCESSFUL_LOAN_DISBURSEMENT)
                                            .types(List.of(NotificationTypeEnum.SMS, NotificationTypeEnum.EMAIL))
                                            .templateParamValues(Map.of(
                                                    "AMOUNT", finalRepaymentSchedulingEvent.getTotalLoanAmount(),
                                                    "DUE_DATE", finalRepaymentSchedulingEvent.getDueDate(),
                                                    "TOTAL_INTEREST", finalRepaymentSchedulingEvent.getTotalInterest()))
                                            .recipient(NotificationCommand.Recipient.builder()
                                                    .msisdn("254742887480")
                                                    .to(List.of("ohtischris@gmail.com"))
                                                    .build())
                                            .build();
                                    payloadString = objectMapper.writeValueAsString(command);
                                }
                            } catch (Exception e) {
                                Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error creating notification command", e);
                                throw new RuntimeException(e);
                            }

                            saga.setCurrentStep(nextStep);

                            OutBoxEvent outBoxEvent = OutBoxEvent.builder()
                                    .aggregateType(LOAN_AGGREGATE)
                                    .aggregateId(loanId.toString())
                                    .createdAt(now())
                                    .isPublished(false)
                                    .payload(payloadString)
                                    .eventType(CommandsEnum.NOTIFY_COMMAND.name())
                                    .build();

                            return updateSagaStepStatus(payload, sagaStep)
                                    .flatMap(step -> {
                                        return stepRepo.save(step)
                                                .then(stepRepo.save(nextStep(saga)))
                                                .then(outboxRepo.save(outBoxEvent))
                                                .then(sagaRepo.save(saga));
                                    });

                        }).then()));

    }


    @KafkaListener(topics = {"loan.repayment.prevalidation.event"}, groupId = "orchestrator")
    public Mono<Void> onRepaymentPrevalidationEvent(String payload) {

        RepaymentEvent repaymentEvent;
        try {
            repaymentEvent = objectMapper.readValue(payload, RepaymentEvent.class);
        } catch (Exception e) {
            Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing loan.repayment.prevalidation.event", e);
            return Mono.empty();
        }

        UUID loanId = repaymentEvent.getCommandId();
        Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from loan.repayment.prevalidation.event: " + payload, null);

        RepaymentEvent finalRepaymentEvent = repaymentEvent;
        boolean notSuccessful = !ResponseCodes.RC_200.name().equalsIgnoreCase(finalRepaymentEvent.getStatus());
        return sagaRepo.findByBusinessKey(loanId)
                .flatMap(saga -> stepRepo.findBySagaIdAndStepName(saga.getId(), LOAN_VALIDATION_STEP)
                        .collectList()
                        .flatMap(sagaStep -> tx.execute(status -> {

                            GenericRequest<DefaultRequestHeader, LoanRepaymentRequest> originalRequest;
                            try {
                                originalRequest = objectMapper.readValue(saga.getOriginalRequest(), new TypeReference<GenericRequest<DefaultRequestHeader, LoanRepaymentRequest>>() {
                                });
                            } catch (Exception e) {
                                Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing loan application request", e);
                                throw new RuntimeException(e);
                            }

                            saga.setUpdatedAt(now());

                            String payloadString;
                            OutBoxEvent outBoxEvent;
                            String nextStep;
                            try {
                                if (notSuccessful) {
                                    nextStep = NOTIFY_CUSTOMER_STEP;
                                    String message = finalRepaymentEvent.getMessage();
                                    Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Sending notification event: " + message, null);
                                    NotificationCommand notificationCommand = NotificationCommand.builder()
                                            .commandId(loanId)
                                            .types(List.of(NotificationTypeEnum.SMS, NotificationTypeEnum.EMAIL))
                                            .template(REPAYMENT_PREVALIDATION_FAILED_TEMPLATE)
                                            .recipient(NotificationCommand.Recipient.builder().msisdn("254742887480").to(List.of("abc@xyz.com")).build())
                                            .templateParamValues(Map.of("CUSTOMER_MESSAGE", message == null ? "Disbursement failed" : message))
                                            .build();
                                    payloadString = objectMapper.writeValueAsString(notificationCommand);
                                    outBoxEvent = OutBoxEvent.builder()
                                            .aggregateType(LOAN_AGGREGATE)
                                            .aggregateId(loanId.toString())
                                            .createdAt(now())
                                            .isPublished(false)
                                            .payload(payloadString)
                                            .eventType(CommandsEnum.NOTIFY_COMMAND.name())
                                            .build();
                                } else {
                                    Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "saving loan repayment command" + finalRepaymentEvent.getMessage(), null);

                                    nextStep = LOAN_REPAYMENT_STEP;
                                    RepaymentCommand command = RepaymentCommand.builder()
                                            .commandId(loanId)
                                            .amount(originalRequest.getBody().getAmount())
                                            .loanScheduleId(originalRequest.getBody().getLoanScheduleId())
                                            .walletType(originalRequest.getBody().getWalletType())
                                            .walletId(originalRequest.getBody().getWalletId())
                                            .isValidated(true)
                                            .build();
                                    payloadString = objectMapper.writeValueAsString(command);
                                    outBoxEvent = OutBoxEvent.builder()
                                            .aggregateType(LOAN_AGGREGATE)
                                            .aggregateId(loanId.toString())
                                            .createdAt(now())
                                            .isPublished(false)
                                            .payload(payloadString)
                                            .eventType(CommandsEnum.REPAYMENT_COMMAND.name())
                                            .build();

                                }
                            } catch (Exception e) {
                                Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error creating notification command", e);
                                throw new RuntimeException(e);
                            }

                            saga.setCurrentStep(nextStep);

                            return updateSagaStepStatus(payload, sagaStep)
                                    .flatMap(step -> {
                                        return stepRepo.save(step)
                                                .then(stepRepo.save(nextStep(saga)))
                                                .then(outboxRepo.save(outBoxEvent))
                                                .then(sagaRepo.save(saga));
                                    });

                        }).then()));

    }


    @KafkaListener(topics = {"loan.repayment.event"}, groupId = "orchestrator")
    public Mono<Void> onRepaymentEvent(String payload) {

        RepaymentEvent repaymentEvent;
        try {
            repaymentEvent = objectMapper.readValue(payload, RepaymentEvent.class);
        } catch (Exception e) {
            Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing loan.repayment.event: " + payload, e);
            return Mono.empty();
        }

        UUID loanId = repaymentEvent.getCommandId();
        Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from loan.repayment.event: " + payload, null);

        RepaymentEvent finalRepaymentEvent = repaymentEvent;
        boolean notSuccessful = !ResponseCodes.RC_200.name().equalsIgnoreCase(finalRepaymentEvent.getStatus());
        return sagaRepo.findByBusinessKey(loanId)
                .flatMap(saga -> stepRepo.findBySagaIdAndStepName(saga.getId(), LOAN_REPAYMENT_STEP)
                        .collectList()
                        .flatMap(sagaStep -> tx.execute(status -> {

                            GenericRequest<DefaultRequestHeader, LoanRepaymentRequest> originalRequest;
                            try {
                                originalRequest = objectMapper.readValue(saga.getOriginalRequest(), new TypeReference<GenericRequest<DefaultRequestHeader, LoanRepaymentRequest>>() {
                                });
                            } catch (Exception e) {
                                Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing loan application request", e);
                                throw new RuntimeException(e);
                            }


                            saga.setUpdatedAt(now());

                            String payloadString;
                            OutBoxEvent outBoxEvent;
                            String nextStep;
                            try {
                                if (notSuccessful) {
                                    nextStep = NOTIFY_CUSTOMER_STEP;
                                    String message = finalRepaymentEvent.getMessage();
                                    Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Sending notification event: " + message, null);
                                    NotificationCommand notificationCommand = NotificationCommand.builder()
                                            .commandId(loanId)
                                            .types(List.of(NotificationTypeEnum.SMS, NotificationTypeEnum.EMAIL))
                                            .template(REPAYMENT_PREVALIDATION_FAILED_TEMPLATE)
                                            .recipient(NotificationCommand.Recipient.builder().msisdn("254742887480").to(List.of("abc@xyz.com")).build())
                                            .templateParamValues(Map.of("CUSTOMER_MESSAGE", message == null ? "Disbursement failed" : message))
                                            .build();
                                    payloadString = objectMapper.writeValueAsString(notificationCommand);
                                    outBoxEvent = OutBoxEvent.builder()
                                            .aggregateType(LOAN_AGGREGATE)
                                            .aggregateId(loanId.toString())
                                            .createdAt(now())
                                            .isPublished(false)
                                            .payload(payloadString)
                                            .eventType(CommandsEnum.NOTIFY_COMMAND.name())
                                            .build();
                                } else {
                                    Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "saving loan repayment command" + finalRepaymentEvent.getMessage(), null);
                                    nextStep = NOTIFY_CUSTOMER_STEP;

                                    String message = finalRepaymentEvent.getMessage();
                                    Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Sending notification event: " + message, null);
                                    NotificationCommand notificationCommand = NotificationCommand.builder()
                                            .commandId(loanId)
                                            .types(List.of(NotificationTypeEnum.SMS, NotificationTypeEnum.EMAIL))
                                            .template(REPAYMENT_SUCCESSFUL_TEMPLATE)
                                            .recipient(NotificationCommand.Recipient.builder().msisdn("254742887480").to(List.of("abc@xyz.com")).build())
                                            .templateParamValues(Map.of("CUSTOMER_MESSAGE", message == null ? "Repayment success" : message, "amount", originalRequest.getBody().getAmount().toString()))
                                            .build();
                                    payloadString = objectMapper.writeValueAsString(notificationCommand);
                                    outBoxEvent = OutBoxEvent.builder()
                                            .aggregateType(LOAN_AGGREGATE)
                                            .aggregateId(loanId.toString())
                                            .createdAt(now())
                                            .isPublished(false)
                                            .payload(payloadString)
                                            .eventType(CommandsEnum.NOTIFY_COMMAND.name())
                                            .build();


                                }
                            } catch (Exception e) {
                                Helpers.log(loanId.toString(), LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error creating notification command", e);
                                throw new RuntimeException(e);
                            }


                            saga.setCurrentStep(nextStep);

                            return updateSagaStepStatus(payload, sagaStep)
                                    .flatMap(step -> {
                                        return stepRepo.save(step)
                                                .then(stepRepo.save(nextStep(saga)))
                                                .then(outboxRepo.save(outBoxEvent))
                                                .then(sagaRepo.save(saga));
                                    });


                        }).then()));

    }

    @KafkaListener(topics = {"notification.event"}, groupId = "orchestrator")
    public Mono<Void> onNotificationEvent(String payload) {

        NotificationEvent repaymentEvent;
        try {
            repaymentEvent = objectMapper.readValue(payload, NotificationEvent.class);
            Helpers.log(repaymentEvent.getCommandId().toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Parsed notification.event: " + payload, null);
        } catch (Exception e) {
            Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error parsing notification.event", e);
            return Mono.empty();
        }

        UUID loanId = repaymentEvent.getCommandId();
        Helpers.log(loanId.toString(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from notification.event: " + payload, null);
        return sagaRepo.findByBusinessKey(loanId)
                .flatMap(saga -> stepRepo.findBySagaIdAndStepName(saga.getId(), NOTIFY_CUSTOMER_STEP)
                        .collectList()
                        .flatMap(sagaStep -> tx.execute(status -> {

                            saga.setCurrentStep("COMPLETED");
                            saga.setUpdatedAt(now());

                            return updateSagaStepStatus(payload, sagaStep)
                                    .flatMap(step -> {
                                        return stepRepo.save(step)
                                                .then(sagaRepo.save(saga));
                                    });

                        }).then()));

    }

    private @NonNull Mono<SagaStep> updateSagaStepStatus(String payload, List<SagaStep> sagaSteps) {
        if (sagaSteps == null || sagaSteps.isEmpty()) {
            return Mono.empty();
        }

        SagaStep first = sagaSteps.getFirst();

        if (sagaSteps.size() > 1) {
            sagaSteps.remove(first);

            first.setStatus(COMPLETED_STATUS);
            first.setOutcome(payload);
            return stepRepo.deleteAll(sagaSteps)
                    .thenReturn(first);
        } else {
            first.setStatus(COMPLETED_STATUS);
            first.setOutcome(payload);
            return Mono.just(first);
        }
    }

    private SagaStep nextStep(Saga saga) {
        Helpers.log(saga.getBusinessKey(), LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Creating next step for saga: " + saga.getId(), null);
        return SagaStep.builder()
                .status("REQUESTED")
                .stepName(saga.getCurrentStep())
                .sagaId(saga.getId())
                .retryCount(0)
                .executedAt(now())
                .build();
    }


}
