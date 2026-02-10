package ke.co.interviewusercaseworld.repayment.model.dto.requests;

import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureOptionsTypeEnum;
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
public class LoanRepaymentSchedulingDto {
    private UUID loanId;
    private UUID customerId;
    private UUID productId;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private InterestRateTypeEnum interestRateType;
    private Integer tenure;
    private TenureOptionsTypeEnum tenureType;
    private Boolean isInstallment;
}
