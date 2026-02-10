package ke.co.interviewusercaseworld.repayment.model.entities;

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

@Table(schema = "repayments", value = "repayments")
public class Repayment {

    @Id
    private UUID repaymentId;
    private UUID loanId;
    private UUID scheduleId;
    private BigDecimal amountPaid;
    private LocalDateTime paymentDate;
    private String paymentMode;
    private String status;
}
