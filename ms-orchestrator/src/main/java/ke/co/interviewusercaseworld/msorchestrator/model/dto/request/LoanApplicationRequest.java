package ke.co.interviewusercaseworld.msorchestrator.model.dto.request;

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
public class LoanApplicationRequest {
    private BigDecimal loanAmount;
    private UUID productId;
    private UUID customerId;
    private String loanPurpose;
    private Integer tenure;
    @Builder.Default
    private WalletTypeEnum walletType = WalletTypeEnum.MobileMoney;
    private String walletId;
    @Builder.Default
    private Boolean installment = false;
}
