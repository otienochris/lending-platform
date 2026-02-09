package ke.co.expd.authserver.repoisitory;

import ke.co.expd.authserver.model.entities.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    @Query("SELECT * FROM auth.auth_users order by id offset :offset rows fetch next :limit rows only ")
    Flux<User> findAllByPageable(BigInteger offset, BigInteger limit);

    Mono<User> findByUsername(String username);
}
