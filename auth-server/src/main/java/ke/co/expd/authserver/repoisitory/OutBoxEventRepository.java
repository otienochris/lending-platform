package ke.co.expd.authserver.repoisitory;

import ke.co.expd.authserver.model.entities.OutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OutBoxEventRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    Flux<OutboxEvent> findTop20ByIsPublishedFalse();

}
