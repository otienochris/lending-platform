package ke.co.interviewusercaseworld.msorchestrator.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicationRequest {
    private String loanAmount;
    private String productId;
    private String customerId;
    private String loanPurpose;
    private String tenure;
}
