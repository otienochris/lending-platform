package ke.co.interviewusercaseworld.repayment.repository;

import ke.co.interviewusercaseworld.repayment.model.entities.RepaymentSchedule;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface RepaymentScheduleRepository extends ReactiveCrudRepository<RepaymentSchedule, UUID> {
    Flux<RepaymentSchedule> findAllByLoanId(UUID loanId);

    Flux<RepaymentSchedule> findAllByLoanIdAndStatusNotIn(UUID id, List<String> status);
}
