package ke.co.interviewusercaseworld.msorchestrator.repository;

import ke.co.interviewusercaseworld.msorchestrator.model.entities.Saga;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SagaRepository extends ReactiveCrudRepository<Saga, UUID> {
    Mono<Saga> findByBusinessKey(UUID loanId);

    Flux<Saga> findAllBySagaTypeAndBusinessKey(String sagaType, UUID businessKey);
}
