package ke.co.interviewusercaseworld.scheduler.repository;

import ke.co.interviewusercaseworld.scheduler.model.entity.RepaymentSchedule;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RepaymentScheduleRepository extends ReactiveCrudRepository<RepaymentSchedule, UUID> {
    Flux<RepaymentSchedule> findTop50ByDueDateAfterAndDueDateBefore(LocalDateTime dayBefore, LocalDateTime now);


    /**
     * SELECT *
     * FROM repayments.repayment_schedule
     * WHERE due_date > '2026-02-01T00:00:00'
     * AND due_date < '2026-02-28T23:59:59'
     * AND status = 'PENDING'
     * ORDER BY due_date
     * LIMIT 50;
     *
     * @param localDateTime
     * @param now
     * @param name
     * @return
     */

    @Query("""
                SELECT *
                FROM repayments.repayment_schedule
                WHERE due_date > :start
                  AND due_date < :end
                  AND status = :status
                ORDER BY due_date
                LIMIT 50
            """)
    Flux<RepaymentSchedule> findTop50ByDueDateAfterAndDueDateBeforeAndStatus(LocalDateTime localDateTime, LocalDateTime now, String name);

    Flux<RepaymentSchedule> findTop50ByStatusAndDueDateBetweenOrderByDueDateAsc(String name, LocalDateTime from, LocalDateTime to);

    Mono<RepaymentSchedule> findTop50ByStatusAndDueDateBefore(String name, LocalDateTime now);

    Flux<RepaymentSchedule> findTop50ByStatusNotAndDueDateBefore(String name, LocalDateTime now);
}
