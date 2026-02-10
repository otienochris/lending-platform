package ke.co.interviewusercaseworld.repayment.model.dto.requests;

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
public class DisbursementRequest {
    private UUID productId;
    private UUID loanId;
    private BigDecimal amount;
    private UUID customerId;
    private String walletId;
    @Builder.Default
    private WalletTypeEnum destinationWallet = WalletTypeEnum.MobileMoney;
}
