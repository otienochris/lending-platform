package ke.co.interviewusercaseworld.msorchestrator.model.dto.request;

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
}
