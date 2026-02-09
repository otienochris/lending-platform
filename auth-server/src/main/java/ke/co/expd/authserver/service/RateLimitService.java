package ke.co.expd.authserver.service;

import reactor.core.publisher.Mono;

public interface RateLimitService {

    Mono<Boolean> checkRateLimit(String ipAddress, String endpoint);
}
