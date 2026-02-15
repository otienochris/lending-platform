package ke.co.interviewusercaseworld.repayment.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanRepaymentSchedulingResponse {
    private BigInteger installmentNumber;
    private BigDecimal principalComponent;
    private BigDecimal totalOutstandingAmount;
    private BigDecimal interestComponent;
    private LocalDateTime dueDate;
    private BigDecimal totalInstallment;
    private BigDecimal remainingBalance;

}
