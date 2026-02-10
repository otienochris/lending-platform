package ke.co.interviewusercaseworld.repayment.model.entities;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface LoanRepository extends ReactiveCrudRepository<Loan, UUID> {
}
