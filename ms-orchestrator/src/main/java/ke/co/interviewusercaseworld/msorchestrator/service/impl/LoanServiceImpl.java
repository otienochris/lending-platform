package ke.co.interviewusercaseworld.msorchestrator.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.commands.ProductValidationCommand;
import ke.co.interviewusercaseworld.commons.dto.commands.RepaymentCommand;
import ke.co.interviewusercaseworld.commons.dto.commands.RepaymentSchedulingCommand;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.enums.SagaTypeEnum;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanRepaymentRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationAcknowledgement;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanRepaymentRequestAck;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.OutBoxEvent;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.Saga;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.SagaStep;
import ke.co.interviewusercaseworld.msorchestrator.repository.CommandDeduplicationRepo;
import ke.co.interviewusercaseworld.msorchestrator.repository.OutBoxEventRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaStepRepository;
import ke.co.interviewusercaseworld.msorchestrator.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    public static final String LOAN_VALIDATION_STEP = "LOAN_VALIDATION";
    private final CommandDeduplicationRepo commandDeduplicationRepo;
    private final SagaRepository sagaRepository;
    private final SagaStepRepository sagaStepRepository;
    private final OutBoxEventRepository outBoxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>> apply(GenericRequest<DefaultRequestHeader, LoanApplicationRequest> request) {

        // create saga
        UUID sagaId = getSagaId(request.getHeader().getCorrelationId());
        UUID loanId = UUID.randomUUID();

        Saga saga = Saga.builder()
                .sagaType(SagaTypeEnum.LOAN_APPLICATION.name())
                .businessKey(loanId.toString())
                .status("STARTED")
                .currentStep("PRODUCT_VALIDATION")
                .originalRequest(serialize(request))
                .build();

        SagaStep sagaStep = SagaStep.builder()
                .stepName("PRODUCT_VALIDATION")
                .status("REQUESTED")
                .build();

        ProductValidationCommand productValidationCommand = ProductValidationCommand.builder()
                .productId(request.getBody().getProductId())
                .commandId(loanId)
                .loanAmount(request.getBody().getLoanAmount())
                .installment(request.getBody().getInstallment())
                .build();

        OutBoxEvent outBoxEvent = OutBoxEvent.builder()
                .aggregateType("LOAN")
                .aggregateId(loanId.toString())
                .createdAt(LocalDateTime.now())
                .isPublished(false)
                .payload(serialize(productValidationCommand))
                .eventType(CommandsEnum.PRODUCT_VALIDATION_COMMAND.name())
                .build();

        return saveSagaAndStep(saga, sagaStep, outBoxEvent)
                .collectList()
                .flatMap(outBoxEvents -> {
                    return Mono.just(
                            GenericResponse.<DefaultResponseHeader, LoanApplicationAcknowledgement>builder()
                                    .header(DefaultResponseHeader.builder()
                                            .responseRefId(request.getHeader().getRequestRefId())
                                            .responseCode(ResponseCodes.RC_200)
                                            .operation(OperationNameEnum.LOAN_APPLICATION)
                                            .customerMessage("Loan application initiated successfully.")
                                            .debugMessage("Loan application initiated successfully.")
                                            .correlationId(sagaId)
                                            .build())
                                    .body(LoanApplicationAcknowledgement.builder()
                                            .loanReferenceId(loanId)
                                            .build())
                                    .build()
                    );
                });


    }

    private static @NonNull UUID getSagaId(UUID correlationId) {
        return correlationId == null ? UUID.randomUUID() : correlationId;
    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanRepaymentRequestAck>> repay(GenericRequest<DefaultRequestHeader, LoanRepaymentRequest> request) {

        // create saga
        UUID sagaId = getSagaId(request.getHeader().getCorrelationId());
        UUID loanId = UUID.randomUUID();

        Saga saga = Saga.builder()
                .sagaType(SagaTypeEnum.LOAN_REPAYMENT.name())
                .businessKey(loanId.toString())
                .status("STARTED")
                .currentStep(LOAN_VALIDATION_STEP)
                .originalRequest(serialize(request))
                .build();

        SagaStep sagaStep = SagaStep.builder()
                .stepName(LOAN_VALIDATION_STEP)
                .status("REQUESTED")
                .build();

        RepaymentCommand repaymentCommand = RepaymentCommand.builder()
                .loanScheduleId(request.getBody().getLoanScheduleId())
                .commandId(loanId)
                .amount(request.getBody().getAmount())
                .customerId(request.getBody().getCustomerId())
                .walletId(request.getBody().getWalletId())
                .walletType(request.getBody().getWalletType())
                .build();

        OutBoxEvent outBoxEvent = OutBoxEvent.builder()
                .aggregateType("REPAYMENT")
                .aggregateId(loanId.toString())
                .createdAt(LocalDateTime.now())
                .isPublished(false)
                .payload(serialize(repaymentCommand))
                .eventType(CommandsEnum.REPAYMENT_PREVALIDATION_COMMAND.name())
                .build();
        return saveSagaAndStep(saga, sagaStep, outBoxEvent)
                .collectList()
                .flatMap(outBoxEvents -> {
                    return Mono.just(
                            GenericResponse.<DefaultResponseHeader, LoanRepaymentRequestAck>builder()
                                    .header(DefaultResponseHeader.builder()
                                            .responseRefId(request.getHeader().getRequestRefId())
                                            .responseCode(ResponseCodes.RC_200)
                                            .operation(OperationNameEnum.LOAN_APPLICATION)
                                            .customerMessage("Loan repayment initiated successfully.")
                                            .debugMessage("Loan repayment initiated successfully.")
                                            .correlationId(sagaId)
                                            .build())
                                    .body(LoanRepaymentRequestAck.builder()
                                            .referenceNumber(loanId)
                                            .build())
                                    .build()
                    );
                });
    }

    private @NonNull Flux<OutBoxEvent> saveSagaAndStep(Saga saga, SagaStep sagaStep, OutBoxEvent outBoxEvent) {
        return transactionalOperator.execute(tx ->
                sagaRepository.save(saga)
                        .flatMap(s -> {
                            sagaStep.setSagaId(s.getId());
                            return sagaStepRepository.save(sagaStep);
                        })
                        .flatMap(step -> outBoxEventRepository.save(outBoxEvent))
                        .doOnError(error -> log.error("Error saving saga: {}", error.getMessage()))
        );
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
