package ke.co.expd.authserver.repoisitory;

import ke.co.expd.authserver.model.entities.UserRole;
import ke.co.expd.authserver.model.entities.UserRoleId;
import org.apache.el.stream.Stream;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRoleRepository extends ReactiveCrudRepository<UserRole, UserRoleId> {
    @Query("select * from auth.user_roles where user_id=:userId")
    Flux<UserRole> findByUserId(UUID userId);
}
