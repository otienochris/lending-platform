package ke.co.interviewusercaseworld.scheduler.repository;

import ke.co.interviewusercaseworld.scheduler.model.entity.ProductConfigurationOutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ProductConfigurationOutBoxEventRepository extends ReactiveCrudRepository<ProductConfigurationOutboxEvent, UUID> {
    Flux<ProductConfigurationOutboxEvent> findTop20ByIsPublishedFalse();

}
