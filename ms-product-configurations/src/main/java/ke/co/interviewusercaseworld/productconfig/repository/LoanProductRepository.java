package ke.co.interviewusercaseworld.productconfig.repository;

import ke.co.interviewusercaseworld.commons.entities.product.LoanProduct;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface LoanProductRepository extends ReactiveCrudRepository<LoanProduct, UUID> {

    Flux<LoanProduct> findByActiveTrue();

    Mono<Boolean> existsByProductName(String productName);
}
