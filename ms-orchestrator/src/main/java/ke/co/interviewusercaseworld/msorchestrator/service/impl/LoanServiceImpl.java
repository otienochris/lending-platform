package ke.co.interviewusercaseworld.msorchestrator.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.enums.SagaTypeEnum;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationAcknowledgement;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final CommandDeduplicationRepo commandDeduplicationRepo;
    private final SagaRepository sagaRepository;
    private final SagaStepRepository sagaStepRepository;
    private final OutBoxEventRepository outBoxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>> apply(GenericRequest<DefaultRequestHeader, LoanApplicationRequest> request) {

        // create saga
        UUID sagaId = request.getHeader().getCorrelationId();
        if (sagaId == null) {
            sagaId = UUID.randomUUID();
        }


        UUID loanId = UUID.randomUUID();

        UUID finalSagaId = sagaId;

        Saga saga = Saga.builder()
                .sagaType(SagaTypeEnum.LOAN_APPLICATION.name())
                .businessKey(loanId.toString())
                .status("STARTED")
                .currentStep("PRODUCT_VALIDATION")
                .build();

        SagaStep sagaStep = SagaStep.builder()
                .stepName("PRODUCT_VALIDATION")
                .status("REQUESTED")
                .build();

        OutBoxEvent outBoxEvent = OutBoxEvent.builder()
                .aggregateType("LOAN")
                .aggregateId(loanId.toString())
                .createdAt(LocalDateTime.now())
                .isPublished(false)
                .payload(serialize(request))
                .eventType(CommandsEnum.PRODUCT_VALIDATION_COMMAND.name())
                .build();

        return transactionalOperator.execute(tx ->
                        sagaRepository.save(saga)
                                .flatMap(s -> {
                                    sagaStep.setSagaId(s.getId());
                                    return sagaStepRepository.save(sagaStep);
                                })
                                .flatMap(step -> outBoxEventRepository.save(outBoxEvent))
                                .doOnError(error -> log.error("Error saving saga: {}", error.getMessage()))
                )
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
                                            .correlationId(finalSagaId)
                                            .build())
                                    .body(LoanApplicationAcknowledgement.builder()
                                            .loanReferenceId(loanId)
                                            .build())
                                    .build()
                    );
                });


    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
