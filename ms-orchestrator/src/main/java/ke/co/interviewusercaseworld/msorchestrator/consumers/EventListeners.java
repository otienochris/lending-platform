package ke.co.interviewusercaseworld.msorchestrator.consumers;

import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.events.ProductValidationEvent;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.OutBoxEvent;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.Saga;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.SagaStep;
import ke.co.interviewusercaseworld.msorchestrator.repository.OutBoxEventRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaRepository;
import ke.co.interviewusercaseworld.msorchestrator.repository.SagaStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@Component
@RequiredArgsConstructor
public class EventListeners {

    private final ObjectMapper objectMapper;
    private final SagaRepository sagaRepo;
    private final TransactionalOperator tx;
    private final SagaStepRepository stepRepo;
    private final OutBoxEventRepository outboxRepo;


    @KafkaListener(topics = {"product.validation.event"}, groupId = "orchestrator")
    public Mono<Void> onDisbursementEvent(String payload){
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.KAFKA_CONSUMER, "Received message from kafka: " + payload, null);
        ProductValidationEvent productValidationEvent = objectMapper.readValue(payload, ProductValidationEvent.class);

        UUID loanId = productValidationEvent.getLoanId();
        return sagaRepo.findByBusinessKey(loanId)
                .flatMap(saga -> stepRepo.findBySagaIdAndStepName(saga.getId(),"PRODUCT_VALIDATION")
                        .flatMap(sagaStep -> tx.execute(status -> {
                                    String currentStage = CommandsEnum.PRODUCT_VALIDATION_COMMAND.name();


                                    saga.setCurrentStep(currentStage);
                                    saga.setUpdatedAt(now());

                                    sagaStep.setStatus("COMPLETED");

                                    Map<String, UUID> request = Map.of("userId", loanId);
                                    OutBoxEvent outBoxEvent = OutBoxEvent.builder()
                                            .aggregateType("LOAN")
                                            .aggregateId(loanId.toString())
                                            .createdAt(LocalDateTime.now())
                                            .isPublished(false)
                                            .payload(objectMapper.writeValueAsString(request))
                                            .eventType(currentStage)
                                            .build();

                                    return stepRepo.save(sagaStep)
                                            .then(stepRepo.save(nextStep(saga, CommandsEnum.USER_VALIDATION_COMMAND.name())))
                                            .then(outboxRepo.save(outBoxEvent))
                                            .then(sagaRepo.save(saga));
                                }).then())
                );
    }

    private SagaStep completedStep(SagaStep sagaStep) {
        sagaStep.setStatus("COMPLETED");
        return sagaStep;
    }

    private SagaStep nextStep(Saga saga, String step) {
        return new SagaStep(
                null,
                saga.getId(),
                step,
                "REQUESTED",
                0,
                null,
                now()
        );
    }


}
