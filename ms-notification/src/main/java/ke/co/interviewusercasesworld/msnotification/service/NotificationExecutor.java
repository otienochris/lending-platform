package ke.co.interviewusercasesworld.msnotification.service;

import ke.co.interviewusercasesworld.msnotification.model.dto.response.NotificationResponseDto;
import ke.co.interviewusercasesworld.msnotification.model.entity.Notification;
import reactor.core.publisher.Mono;

public interface NotificationExecutor {

    Mono<NotificationResponseDto> send(Notification notification);
}
