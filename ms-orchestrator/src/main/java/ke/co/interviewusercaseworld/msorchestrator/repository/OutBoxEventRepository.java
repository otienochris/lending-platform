package ke.co.interviewusercaseworld.msorchestrator.repository;

import ke.co.interviewusercaseworld.msorchestrator.model.entities.OutBoxEvent;
import org.apache.el.stream.Stream;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OutBoxEventRepository extends ReactiveCrudRepository<OutBoxEvent, UUID> {
    Flux<OutBoxEvent> findTop20ByIsPublishedFalse();

}
