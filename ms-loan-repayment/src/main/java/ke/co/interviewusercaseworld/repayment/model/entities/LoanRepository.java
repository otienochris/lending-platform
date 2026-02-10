package ke.co.interviewusercaseworld.repayment.model.entities;

import ke.co.interviewusercaseworld.commons.enums.LoanStatusEnum;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface LoanRepository extends ReactiveCrudRepository<Loan, UUID> {
    Flux<Loan> findAllByCustomerIdAndStatus(UUID userId, LoanStatusEnum loanStatus);

    Flux<Loan> findAllByCustomerId(UUID userId);
}
