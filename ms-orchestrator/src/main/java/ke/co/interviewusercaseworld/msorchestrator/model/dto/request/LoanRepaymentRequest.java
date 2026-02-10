package ke.co.interviewusercaseworld.msorchestrator.model.dto.request;

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
    private UUID loanId;
    private BigDecimal totalLoanAmount;
    private Integer installments;
    private LocalDateTime dueDate;
    private UUID customerId;
}
