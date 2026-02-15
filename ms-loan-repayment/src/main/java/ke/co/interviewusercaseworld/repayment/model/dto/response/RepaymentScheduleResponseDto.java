package ke.co.interviewusercaseworld.repayment.model.dto.response;

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
public class RepaymentScheduleResponseDto {
    private Integer installmentNumber;
    private UUID scheduleId;
    private UUID loanId;
    private LocalDateTime dueDate;
    private BigDecimal emiAmount;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal remainingBalance;
    private String status;
}
