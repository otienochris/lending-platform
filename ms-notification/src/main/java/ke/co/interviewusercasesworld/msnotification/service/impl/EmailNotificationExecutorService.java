package ke.co.interviewusercasesworld.msnotification.service.impl;

import ke.co.interviewusercasesworld.msnotification.model.dto.response.NotificationResponseDto;
import ke.co.interviewusercasesworld.msnotification.model.entity.Notification;
import ke.co.interviewusercasesworld.msnotification.service.NotificationExecutor;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EmailNotificationExecutorService implements NotificationExecutor {
    @Override
    public Mono<NotificationResponseDto> send(Notification notification) {
        Helpers.log("", LogLevelEnum.info, OperationNameEnum.EMAIL_NOTIFICATION, "Sending email notification", null);
        return Mono.just(NotificationResponseDto.builder()
                        .success(true)
                .build());
    }
}
