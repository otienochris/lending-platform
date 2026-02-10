package ke.co.interviewusercaseworld.repayment.repository;

import ke.co.interviewusercaseworld.repayment.model.entities.RepaymentSchedule;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface RepaymentScheduleRepository extends ReactiveCrudRepository<RepaymentSchedule, UUID> {
}
