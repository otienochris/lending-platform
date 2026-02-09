package ke.co.expd.authserver.repoisitory;

import ke.co.expd.authserver.model.entities.Role;
import ke.co.expd.authserver.model.entities.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleRepository extends ReactiveCrudRepository<Role, UUID> {
    Mono<Role> findByName(String roleUser);
}
