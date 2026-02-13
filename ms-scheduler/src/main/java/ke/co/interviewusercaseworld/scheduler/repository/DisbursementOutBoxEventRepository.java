package ke.co.interviewusercaseworld.scheduler.repository;

import ke.co.interviewusercaseworld.scheduler.model.entity.DisbursementOutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface DisbursementOutBoxEventRepository extends ReactiveCrudRepository<DisbursementOutboxEvent, UUID> {
    Flux<DisbursementOutboxEvent> findTop20ByIsPublishedFalse();

}
