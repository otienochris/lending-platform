package ke.co.interviewusercaseworld.repayment.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(schema = "repayments", value = "loans")
public class Loan {

    @Id
    private UUID id;

    @Column("loan_id")
    private UUID loanId;

    @Column("customer_id")
    private UUID customerId;

    @Column("principal_amount")
    private BigDecimal principalAmount;

    @Column("interest_rate")
    private BigDecimal interestRate;

    @Column("tenure_months")
    private Integer tenureMonths;

    @Column("outstanding_amount")
    private BigDecimal outstandingAmount;

    @Column("status")
    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;
}
