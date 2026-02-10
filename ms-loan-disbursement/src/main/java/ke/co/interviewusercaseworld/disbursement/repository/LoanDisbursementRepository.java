package ke.co.interviewusercaseworld.disbursement.repository;

import ke.co.interviewusercaseworld.disbursement.model.entities.LoanDisbursement;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface LoanDisbursementRepository extends ReactiveCrudRepository<LoanDisbursement, UUID> {
}
