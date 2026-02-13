package ke.co.interviewusercaseworld.scheduler.repository;

import ke.co.interviewusercaseworld.scheduler.model.entity.NotificationOutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface NotificationOutBoxEventRepository extends ReactiveCrudRepository<NotificationOutboxEvent, UUID> {
    Flux<NotificationOutboxEvent> findTop20ByIsPublishedFalse();

}
