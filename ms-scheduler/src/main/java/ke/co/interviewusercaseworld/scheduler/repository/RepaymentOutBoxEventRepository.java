package ke.co.interviewusercaseworld.scheduler.repository;

import ke.co.interviewusercaseworld.scheduler.model.entity.RepaymentOutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface RepaymentOutBoxEventRepository extends ReactiveCrudRepository<RepaymentOutboxEvent, UUID> {
    Flux<RepaymentOutboxEvent> findTop20ByIsPublishedFalse();

}
