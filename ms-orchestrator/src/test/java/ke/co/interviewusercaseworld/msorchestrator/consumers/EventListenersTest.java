package ke.co.interviewusercaseworld.msorchestrator.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.events.ProductValidationEvent;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.msorchestrator.configs.TestTxConfig;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.Saga;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.SagaStep;
import ke.co.interviewusercaseworld.msorchestrator.repository.OutBoxEventRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaStepRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Import(TestTxConfig.class)
class EventListenersTest {

    @Mock
    SagaRepository sagaRepo;

    @Mock
    SagaStepRepository stepRepo;

    @Mock
    OutBoxEventRepository outboxRepo;

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    TransactionalOperator tx;

    EventListeners listeners;

    @BeforeEach
    void setup() {
        listeners = new EventListeners(
                objectMapper,
                sagaRepo,
                tx,
                stepRepo,
                outboxRepo
        );
    }

    @Test
    void shouldCreateUserValidationCommandWhenProductValidationIsSuccessful() {
        UUID loanId = UUID.randomUUID();

        ProductValidationEvent event = ProductValidationEvent.builder()
                .commandId(loanId)
                .status(ResponseCodes.RC_200.name())
                .message("OK")
                .build();

        Saga saga = Saga.builder()
                .id(UUID.randomUUID())
                .businessKey(loanId.toString())
                .currentStep(EventListeners.PRODUCT_VALIDATION_STEP)
                .originalRequest("""
                {
                  "header": {},
                  "body": {
                    "customerId": "cust1",
                    "loanAmount": 1000
                  }
                }
            """)
                .build();

        SagaStep step = SagaStep.builder()
                .id(UUID.randomUUID())
                .sagaId(saga.getId())
                .stepName(EventListeners.PRODUCT_VALIDATION_STEP)
                .status("REQUESTED")
                .build();

        when(sagaRepo.findByBusinessKey(loanId)).thenReturn(Mono.just(saga));
        when(stepRepo.findBySagaIdAndStepName(saga.getId(), EventListeners.PRODUCT_VALIDATION_STEP))
                .thenReturn(Mono.just(step));

        when(stepRepo.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(outboxRepo.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(sagaRepo.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        String payload = "";
        try {
            objectMapper.writeValueAsString(event);
        } catch (Exception e){
            log.error("Error parsing event", e);
        }

        Mono<Void> result = listeners.onProductValidationEvent(payload);

        StepVerifier.create(result)
                .verifyComplete();

        verify(outboxRepo).save(argThat(o ->
                o.getEventType().equals(CommandsEnum.USER_VALIDATION_COMMAND.name())
        ));

        verify(stepRepo, times(2)).save(any());
        verify(sagaRepo).save(any());
    }


    @Test
    void shouldSendNotificationWhenProductValidationFails() throws Exception {
        UUID loanId = UUID.randomUUID();

        ProductValidationEvent event = ProductValidationEvent.builder()
                .commandId(loanId)
                .status(ResponseCodes.RC_400.name())
                .message("Invalid product")
                .build();

        // reuse same saga/step setup

        Mono<Void> result = listeners.onProductValidationEvent(
                objectMapper.writeValueAsString(event)
        );

        StepVerifier.create(result)
                .verifyComplete();

        verify(outboxRepo).save(argThat(o ->
                o.getEventType().equals(CommandsEnum.NOTIFY_COMMAND.name())
        ));
    }


    @Test
    void shouldIgnoreInvalidPayload() {
        StepVerifier.create(
                listeners.onProductValidationEvent("invalid-json")
        ).verifyComplete();

        verifyNoInteractions(sagaRepo, stepRepo, outboxRepo);
    }




}