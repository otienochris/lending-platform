package ke.co.interviewusercaseworld.repayment.services;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface FundTransfer {

    Mono<Boolean> send(BigDecimal amount, String walletId);
    Mono<Boolean> receive(BigDecimal amount, String walletId);
}
