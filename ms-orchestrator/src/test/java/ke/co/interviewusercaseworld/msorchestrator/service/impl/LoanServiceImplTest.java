package ke.co.interviewusercaseworld.msorchestrator.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.RepaymentOptionEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.enums.WalletTypeEnum;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanRepaymentRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationAcknowledgement;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanRepaymentRequestAck;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.Saga;
import ke.co.interviewusercaseworld.msorchestrator.repository.CommandDeduplicationRepo;
import ke.co.interviewusercaseworld.msorchestrator.repository.OutBoxEventRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    CommandDeduplicationRepo commandDeduplicationRepo;

    @Mock
    SagaRepository sagaRepository;

    @Mock
    SagaStepRepository sagaStepRepository;

    @Mock
    OutBoxEventRepository outBoxEventRepository;

    @Mock
    ObjectMapper objectMapper;


    ReactiveTransactionManager txManager = mock(ReactiveTransactionManager.class);

    LoanServiceImpl loanService;

    @BeforeEach
    void setUp() {


        when(txManager.getReactiveTransaction(any()))
                .thenReturn(Mono.just(mock(ReactiveTransaction.class)));

        when(txManager.commit(any()))
                .thenReturn(Mono.empty());

        when(txManager.rollback(any()))
                .thenReturn(Mono.empty());

        TransactionalOperator transactionalOperator =
                TransactionalOperator.create(txManager);

        // transactionalOperator.execute(...) should just run the lambda
        loanService = new LoanServiceImpl(
                commandDeduplicationRepo,
                sagaRepository,
                sagaStepRepository,
                outBoxEventRepository,
                objectMapper,
                transactionalOperator
        );


    }


    @Test
    void apply_success_createsSagaAndReturnsAck() throws Exception {

        var request = TestData.loanApplicationRequest(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        when(sagaRepository.save(any()))
                .thenAnswer(inv -> {
                    Saga saga = inv.getArgument(0);
                    saga.setId(UUID.randomUUID());
                    return Mono.just(saga);
                });

        when(sagaStepRepository.save(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(outBoxEventRepository.save(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>> result =
                loanService.apply(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertEquals(ResponseCodes.RC_200, res.getHeader().getResponseCode());
                    assertNotNull(res.getBody().getLoanReferenceId());
                    assertNotNull(res.getHeader().getCorrelationId());
                })
                .verifyComplete();
    }


    @Test
    void repay_success_createsSagaAndReturnsAck() throws Exception {
        UUID correlationId = UUID.randomUUID();
        var request = TestData.loanRepaymentRequest(correlationId);


        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        when(sagaRepository.save(any()))
                .thenAnswer(inv -> {
                    Saga saga = inv.getArgument(0);
                    saga.setId(UUID.randomUUID());
                    return Mono.just(saga);
                });

        when(sagaStepRepository.save(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(outBoxEventRepository.save(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<GenericResponse<DefaultResponseHeader, LoanRepaymentRequestAck>> result =
                loanService.repay(request);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertEquals(ResponseCodes.RC_200, res.getHeader().getResponseCode());
                    assertEquals(correlationId, res.getHeader().getCorrelationId());
                    assertNotNull(res.getBody().getReferenceNumber());
                })
                .verifyComplete();
    }

    @Test
    void saveSagaAndStep_errorIsPropagated() throws Exception {


        var request = TestData.loanApplicationRequest(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        when(sagaRepository.save(any()))
                .thenReturn(Mono.error(new RuntimeException("DB failure")));

        Mono<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>> result =
                loanService.apply(request);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void serialize_throwsRuntimeException() throws Exception {
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        var request = TestData.loanApplicationRequest(null);

        assertThrows(RuntimeException.class, () -> loanService.apply(request));
    }


    class TestData {

        static GenericRequest<DefaultRequestHeader, LoanApplicationRequest>
        loanApplicationRequest(UUID correlationId) {

            return GenericRequest.<DefaultRequestHeader, LoanApplicationRequest>builder()
                    .header(DefaultRequestHeader.builder()
                            .correlationId(correlationId)
                            .requestRefId("req-123")
                            .build())
                    .body(LoanApplicationRequest.builder()
                            .productId(UUID.randomUUID())
                            .loanAmount(BigDecimal.valueOf(1000))
                            .repaymentOption(RepaymentOptionEnum.INSTALLMENT)
                            .build())
                    .build();
        }

        static GenericRequest<DefaultRequestHeader, LoanRepaymentRequest>
        loanRepaymentRequest(UUID correlationId) {

            return GenericRequest.<DefaultRequestHeader, LoanRepaymentRequest>builder()
                    .header(DefaultRequestHeader.builder()
                            .correlationId(correlationId)
                            .requestRefId("req-456")
                            .build())
                    .body(LoanRepaymentRequest.builder()
                            .loanScheduleId(UUID.randomUUID())
                            .amount(BigDecimal.valueOf(500))
                            .customerId(UUID.randomUUID())
                            .walletId(UUID.randomUUID().toString())
                            .walletType(WalletTypeEnum.MobileMoney)
                            .build())
                    .build();
        }
    }


}