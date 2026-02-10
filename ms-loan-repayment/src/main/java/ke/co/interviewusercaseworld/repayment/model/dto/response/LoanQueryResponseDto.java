package ke.co.interviewusercaseworld.repayment.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanQueryResponseDto {
    @Id
    private UUID id;
    private UUID loanId;
    private UUID customerId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal outstandingAmount;
    private String status;
    private LocalDateTime createdAt;
    private List<RepaymentScheduleResponseDto> loanRepaymentSchedule;

}
