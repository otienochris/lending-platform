package ke.co.interviewusercaseworld.msorchestrator.configs;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestTxConfig {

    @Bean
    ReactiveTransactionManager reactiveTransactionManager() {
        return new ReactiveTransactionManager() {
            @Override
            public Mono<ReactiveTransaction> getReactiveTransaction(TransactionDefinition definition) {
                return Mono.just(mock(ReactiveTransaction.class));
            }

            @Override
            public Mono<Void> commit(ReactiveTransaction transaction) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> rollback(ReactiveTransaction transaction) {
                return Mono.empty();
            }
        };
    }

    @Bean
    TransactionalOperator transactionalOperator(ReactiveTransactionManager tm) {
        return TransactionalOperator.create(tm);
    }
}
