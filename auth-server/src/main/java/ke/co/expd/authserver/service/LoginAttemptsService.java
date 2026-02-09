package ke.co.expd.authserver.service;

import reactor.core.publisher.Mono;

public interface LoginAttemptsService {
    Mono<Boolean> recordFailedAttempt(String username, String path);

    Mono<Boolean> clearAttempts(String username, String login);
}
