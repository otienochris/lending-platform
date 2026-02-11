package ke.co.interviewusercasesworld.msnotification.service;

import ke.co.interviewusercasesworld.msnotification.model.dto.response.NotificationResponseDto;
import ke.co.interviewusercaseworld.commons.dto.commands.NotificationCommand;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import reactor.core.publisher.Mono;

public interface NotificationService {
    Mono<GenericResponse<DefaultResponseHeader, NotificationResponseDto>> schedule(NotificationCommand notificationCommand);
}
