package ke.co.interviewusercasesworld.msnotification.repository;

import ke.co.interviewusercasesworld.msnotification.model.entity.Notification;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepository extends ReactiveCrudRepository<Notification, UUID> {

    @Query("""
                SELECT *
                FROM notifications.notifications
                WHERE status = :status
                  AND send_at <= :now
                ORDER BY send_at
                LIMIT 20
                FOR UPDATE SKIP LOCKED
            """)
    Flux<Notification> findTop20ByStatusAndSendAtBefore(String status, LocalDateTime now);
}
