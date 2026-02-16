package ke.co.interviewusercaseworld.msorchestrator.repository;

import ke.co.interviewusercaseworld.msorchestrator.model.entities.SagaStep;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SagaStepRepository extends ReactiveCrudRepository<SagaStep, UUID> {
    Mono<SagaStep> findBySagaId(UUID id);

    Flux<SagaStep> findBySagaIdAndStepName(UUID id, String productValidation);

    Flux<SagaStep> findAllBySagaId(UUID sagaId);
}
