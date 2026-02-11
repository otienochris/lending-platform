package ke.co.interviewusercasesworld.msnotification.jobs;

import ke.co.interviewusercasesworld.msnotification.model.entity.Notification;
import ke.co.interviewusercasesworld.msnotification.repository.NotificationRepository;
import ke.co.interviewusercasesworld.msnotification.service.NotificationExecutor;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationJob {

    private final Map<String, NotificationExecutor> notificationExecutorServices;
    private final NotificationRepository notificationRepository;
    private final TransactionalOperator transactionalOperator;

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotification() {

        transactionalOperator
                .execute(status -> notificationRepository
                        .findTop20ByStatusAndSendAtBefore("PENDING", LocalDateTime.now())
                        .flatMap(this::processNotification))
                .subscribe();

    }

    private Mono<Void> processNotification(Notification notification) {
        Helpers.log("NOTIFICATION_JOB", LogLevelEnum.INFO, OperationNameEnum.NOTIFICATION_JOB, "Sending notification", null);

        NotificationExecutor notificationExecutor = notificationExecutorServices.get(notification.getChannel().toLowerCase() + "NotificationExecutorService");
        if (notificationExecutor == null) {
            Helpers.log("NOTIFICATION_JOB", LogLevelEnum.ERROR, OperationNameEnum.NOTIFICATION_JOB, "Notification service not found for channel: " + notification.getChannel(), null);
            return Mono.empty();
        }
        return notificationExecutor.send(notification)
                .flatMap(notificationResponseDto -> {
                    if (notificationResponseDto.getSuccess()) {
                        notification.setStatus("SENT");
                        notification.setSentAt(LocalDateTime.now());

                        return notificationRepository.save(notification)
                                .doOnError(throwable -> Helpers.log("NOTIFICATION_JOB", LogLevelEnum.ERROR, OperationNameEnum.NOTIFICATION_JOB, "Error occurred updating notification status", new RuntimeException(throwable)))
                                .doOnSuccess(res -> Helpers.log("NOTIFICATION_JOB", LogLevelEnum.INFO, OperationNameEnum.NOTIFICATION_JOB, "Updated notification status", null));
                    } else {

                        Helpers.log("NOTIFICATION_JOB", LogLevelEnum.ERROR, OperationNameEnum.NOTIFICATION_JOB, "Error occurred sending notification", null);
                        return Mono.empty();
                    }
                }).then();
    }
}
