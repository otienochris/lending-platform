package ke.co.interviewusercaseworld.disbursement.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface FundTransfer {

    Mono<Boolean> send(BigDecimal amount, String walletId);
}
