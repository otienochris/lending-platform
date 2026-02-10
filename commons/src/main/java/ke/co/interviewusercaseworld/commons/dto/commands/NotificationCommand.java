package ke.co.interviewusercaseworld.commons.dto.commands;

import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.NotificationTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureOptionsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String template;
    private Map<String, Object> templateParamValues;
    private Recipient principal;
    private String interestRate;
    private InterestRateTypeEnum interestRateType;
    private Integer tenure;
    private TenureOptionsTypeEnum tenureType;

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
