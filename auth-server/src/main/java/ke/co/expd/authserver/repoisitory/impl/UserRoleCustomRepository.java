package ke.co.expd.authserver.repoisitory.impl;

import ke.co.expd.authserver.model.entities.UserRole;
import ke.co.expd.authserver.repoisitory.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserRoleCustomRepository {
    private final DatabaseClient databaseClient;

    public Mono<Boolean> saveUserRole(UserRole userRole) {
        String sql = """
                INSERT INTO auth.user_roles (user_id, role_id) VALUES (:userId, :roleId)
                """;

        return databaseClient
                .sql(sql)
                .bind("userId", userRole.getId().getUserId())
                .bind("roleId", userRole.getId().getRoleId())
                .fetch()
                .rowsUpdated()
                .map(rows -> rows > 0)
                .switchIfEmpty(Mono.just(false));
    }
}
