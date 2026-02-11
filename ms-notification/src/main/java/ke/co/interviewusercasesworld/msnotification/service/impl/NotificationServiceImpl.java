package ke.co.interviewusercasesworld.msnotification.service.impl;

import ke.co.interviewusercasesworld.msnotification.configs.NotificationConfigsProperties;
import ke.co.interviewusercasesworld.msnotification.model.dto.response.NotificationResponseDto;
import ke.co.interviewusercasesworld.msnotification.model.entity.Notification;
import ke.co.interviewusercasesworld.msnotification.repository.NotificationRepository;
import ke.co.interviewusercasesworld.msnotification.service.NotificationService;
import ke.co.interviewusercaseworld.commons.configs.AppProperties;
import ke.co.interviewusercaseworld.commons.dto.commands.NotificationCommand;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.*;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationConfigsProperties notificationConfigsProperties;

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, NotificationResponseDto>> schedule(NotificationCommand notificationCommand) {
        Map<NotificationTemplateEnum, Map<NotificationTypeEnum, AppProperties.TemplateDetails>> notificationTemplates = notificationConfigsProperties.getServiceProperties().getNotificationProperties().getNotificationTemplates();

        NotificationTemplateEnum template = notificationCommand.getTemplate();
        if (template == null) {
            return Mono.just(GenericResponse.<DefaultResponseHeader, NotificationResponseDto>builder()
                            .header(DefaultResponseHeader.builder()
                                    .sourceSystem("UNSPECIFIED")
                                    .correlationId(null)
                                    .responseCode(ResponseCodes.RC_400)
                                    .customerMessage("Template not found")
                                    .debugMessage("Template not found")
                                    .build())

                                    .build());

        }
        if (notificationCommand.getRecipient() == null) {
            return Mono.just(GenericResponse.<DefaultResponseHeader, NotificationResponseDto>builder()
                            .header(DefaultResponseHeader.builder()
                                    .sourceSystem("UNSPECIFIED")
                                    .correlationId(null)
                                    .responseCode(ResponseCodes.RC_400)
                                    .customerMessage("Recipient not found")
                                    .debugMessage("Recipient not found")
                                    .build())
                    .build());
        }
        List<NotificationTypeEnum> types = notificationCommand.getTypes();
        return Flux.fromIterable(types)
                        .flatMap(type -> {

                            Map<NotificationTypeEnum, AppProperties.TemplateDetails> templateDetails = notificationTemplates.get(template);
                            if (templateDetails == null) {
                                Helpers.log("notification.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Template details not found for template " + template, null);
                                return Mono.empty();
                            }



                            AppProperties.TemplateDetails details = templateDetails.get(type);

                            Notification notification = Notification.builder()
                                    .channel(type.name())
                                    .templateCode(details.getTemplate())
                                    .status("PENDING")
                                    .msisdn(notificationCommand.getRecipient().getMsisdn())
                                    .emailsTo(getConcatenatedEmails(notificationCommand.getRecipient().getTo()))
                                    .bcc(getConcatenatedEmails(notificationCommand.getRecipient().getBcc()))
                                    .cc(getConcatenatedEmails(notificationCommand.getRecipient().getCc()))
                                    .send_at(notificationCommand.getScheduledDate())
                                    .payload(getPayload(details.getTemplate(), notificationCommand.getTemplateParamValues()))
                                    .build();
                            return notificationRepository.save(notification);
                        })
                .collectList()
                .defaultIfEmpty(List.of())
                .flatMap(notifications -> {
                    if (notifications.isEmpty()) {
                        return Mono.just(GenericResponse.<DefaultResponseHeader, NotificationResponseDto>builder()
                                        .header(DefaultResponseHeader.builder()
                                                .sourceSystem("UNSPECIFIED")
                                                .correlationId(null)
                                                .responseCode(ResponseCodes.RC_400)
                                                .customerMessage("No notifications scheduled")
                                                .debugMessage("No notifications scheduled")
                                                .build())

                                        .build());
                    }

                    return Mono.just(GenericResponse.<DefaultResponseHeader, NotificationResponseDto>builder()
                                    .header(DefaultResponseHeader.builder()
                                            .sourceSystem("UNSPECIFIED")
                                            .correlationId(null)
                                            .responseCode(ResponseCodes.RC_200)
                                            .customerMessage("Notifications scheduled successfully")
                                            .debugMessage("Notifications scheduled successfully")
                                            .build())
                            .build());
                });


    }

    public static String getPayload(String template, Map<String, Object> templateParamValues) {
        if (templateParamValues == null) {
            return template;
        }

        String[] finalTemplate = {template};
        templateParamValues.forEach((k,v) -> {
            finalTemplate[0] = template.replace("{{" + k + "}}", (String) v);
        });
        return finalTemplate[0];
    }

    private String getConcatenatedEmails(List<String> emails) {
        if (emails == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        emails.forEach(email -> sb.append(email).append(","));
        try{
            emails.remove(sb.lastIndexOf(","));
        } catch (Exception e){
            Helpers.log("notification.command", LogLevelEnum.ERROR, OperationNameEnum.KAFKA_CONSUMER, "Error removing last comma from emails", e);
        }

        return emails.toString();
    }
}
