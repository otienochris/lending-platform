package ke.co.interviewusercaseworld.productconfig.repository;

import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.FeeResponseDto;
import ke.co.interviewusercaseworld.productconfig.model.entity.ProductFee;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductFeeRepository extends ReactiveCrudRepository<ProductFee, UUID> {
    Flux<ProductFee> findByProductId(UUID productId);

    Mono<Boolean> existsByProductIdAndFeeType(UUID id, String feeType);
}
