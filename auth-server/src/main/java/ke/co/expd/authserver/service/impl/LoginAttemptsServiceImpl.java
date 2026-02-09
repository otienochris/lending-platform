package ke.co.expd.authserver.service.impl;

import ke.co.expd.authserver.service.LoginAttemptsService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LoginAttemptsServiceImpl implements LoginAttemptsService {
    @Override
    public Mono<Boolean> recordFailedAttempt(String username, String path) {
        return Mono.just(true);
    }

    @Override
    public Mono<Boolean> clearAttempts(String username, String login) {
        return Mono.just(true);
    }
}
