package ke.co.interviewusercaseworld.msorchestrator.repository;

import ke.co.interviewusercaseworld.msorchestrator.model.entities.SagaStep;
import org.apache.el.stream.Stream;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SagaStepRepository extends ReactiveCrudRepository<SagaStep, UUID> {
    Mono<SagaStep> findBySagaId(UUID id);

    Mono<SagaStep> findBySagaIdAndStepName(UUID id, String productValidation);

}
