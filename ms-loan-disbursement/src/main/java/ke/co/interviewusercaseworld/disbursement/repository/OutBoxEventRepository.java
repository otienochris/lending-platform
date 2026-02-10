package ke.co.interviewusercaseworld.disbursement.repository;

import ke.co.interviewusercaseworld.disbursement.model.entities.OutboxEvent;
import org.apache.el.stream.Stream;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OutBoxEventRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    Flux<OutboxEvent> findTop20ByIsPublishedFalse();

}
