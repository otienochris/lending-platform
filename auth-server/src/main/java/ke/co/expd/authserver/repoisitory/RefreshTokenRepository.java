package ke.co.expd.authserver.repoisitory;

import ke.co.expd.authserver.model.entities.RefreshToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, String> {
    Mono<RefreshToken> findByToken(String tokenId);
}
