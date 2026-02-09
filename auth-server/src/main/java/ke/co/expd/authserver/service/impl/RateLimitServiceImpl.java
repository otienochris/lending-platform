package ke.co.expd.authserver.service.impl;

import ke.co.expd.authserver.service.RateLimitService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RateLimitServiceImpl implements RateLimitService {
    @Override
    public Mono<Boolean> checkRateLimit(String ipAddress, String endpoint) {
        return Mono.just(true);
    }
}
