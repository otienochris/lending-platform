package ke.co.interviewusercasesworld.msnotification.repository;

import ke.co.interviewusercasesworld.msnotification.model.entity.OutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OutBoxEventRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    Flux<OutboxEvent> findTop20ByIsPublishedFalse();

}
