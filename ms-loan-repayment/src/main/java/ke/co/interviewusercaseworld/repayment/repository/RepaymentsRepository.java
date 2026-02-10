package ke.co.interviewusercaseworld.repayment.repository;

import ke.co.interviewusercaseworld.repayment.model.entities.Repayment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface RepaymentsRepository extends ReactiveCrudRepository<Repayment, UUID> {
}
