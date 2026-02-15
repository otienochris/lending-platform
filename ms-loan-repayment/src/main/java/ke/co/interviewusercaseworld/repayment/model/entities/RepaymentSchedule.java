package ke.co.interviewusercaseworld.repayment.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "repayments", value = "repayment_schedule")
public class RepaymentSchedule {
    @Id
    private UUID scheduleId;
    private UUID loanId;
    private LocalDate dueDate;
    private BigDecimal emiAmount;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal totalPaid;
    private String status;
    @Column("schedule_number")
    private Integer installmentNumber;
}
