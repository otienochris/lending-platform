package ke.co.interviewusercasesworld.msnotification.repository;

import ke.co.interviewusercasesworld.msnotification.model.entity.Notification;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface NotificationRepository extends ReactiveCrudRepository<Notification, UUID> {
}
