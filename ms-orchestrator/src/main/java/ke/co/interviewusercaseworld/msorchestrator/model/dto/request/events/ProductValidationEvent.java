package ke.co.interviewusercaseworld.msorchestrator.model.dto.request.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductValidationEvent {
    private UUID commandId;
    private UUID productId;
    private UUID loanId;
}
