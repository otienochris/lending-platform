package ke.co.interviewusercaseworld.msorchestrator.model.dto.request.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentEvent {
    private String loanId;
    private String status;
    private String referenceNumber;
}
