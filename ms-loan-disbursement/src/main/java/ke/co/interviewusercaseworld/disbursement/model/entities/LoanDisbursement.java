package ke.co.interviewusercaseworld.disbursement.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(schema = "disbursements", value = "loan_disbursements")
public class LoanDisbursement {

    @Id
    private UUID disbursementId;
    private UUID loanId;
    private UUID customerId;
    private BigDecimal amount;
    private UUID productId;
    private String status;
    private String referenceNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
