package ke.co.interviewusercaseworld.scheduler.repository;

import ke.co.interviewusercaseworld.scheduler.model.entity.OrchestratorOutBoxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OrchestratorOutBoxEventRepository extends ReactiveCrudRepository<OrchestratorOutBoxEvent, UUID> {
    Flux<OrchestratorOutBoxEvent> findTop20ByIsPublishedFalse();

}
