package ke.co.interviewusercaseworld.scheduler.repository;

import ke.co.interviewusercaseworld.scheduler.model.entity.AuthOutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AuthOutBoxEventRepository extends ReactiveCrudRepository<AuthOutboxEvent, UUID> {
    Flux<AuthOutboxEvent> findTop20ByIsPublishedFalse();

}
