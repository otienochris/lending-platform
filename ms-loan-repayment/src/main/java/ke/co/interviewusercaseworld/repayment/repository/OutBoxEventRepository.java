package ke.co.interviewusercaseworld.repayment.repository;

import ke.co.interviewusercaseworld.repayment.model.entities.OutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OutBoxEventRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    Flux<OutboxEvent> findTop20ByIsPublishedFalse();

}
