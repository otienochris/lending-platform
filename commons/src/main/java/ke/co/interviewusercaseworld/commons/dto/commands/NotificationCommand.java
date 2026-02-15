package ke.co.interviewusercaseworld.commons.dto.commands;

import ke.co.interviewusercaseworld.commons.enums.NotificationTemplateEnum;
import ke.co.interviewusercaseworld.commons.enums.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationCommand {
    private List<NotificationTypeEnum> types;
    private UUID commandId;
    private NotificationTemplateEnum template;
    private Map<String, Object> templateParamValues;
    private Recipient recipient;
    private LocalDateTime scheduledDate;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Recipient {
        private String msisdn;
        private List<String> to;
        private List<String> cc;
        private List<String> bcc;
    }

}
