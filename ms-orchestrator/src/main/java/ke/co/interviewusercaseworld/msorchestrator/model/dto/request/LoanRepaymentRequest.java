package ke.co.interviewusercaseworld.msorchestrator.model.dto.request;

import ke.co.interviewusercaseworld.commons.enums.WalletTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanRepaymentRequest {
    private UUID loanScheduleId;
    private UUID customerId;
    private BigDecimal amount;
    private WalletTypeEnum walletType;
    private String walletId;
}
