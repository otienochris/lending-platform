package ke.co.interviewusercaseworld.commons.dto.commands;

import ke.co.interviewusercaseworld.commons.enums.WalletTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentCommand {
    private UUID commandId;
    private UUID loanScheduleId;
    private UUID customerId;
    private BigDecimal amount;
    private WalletTypeEnum walletType;
    private String walletId;
    @Builder.Default
    private Boolean isValidated = false;
}
