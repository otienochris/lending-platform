package ke.co.interviewusercaseworld.commons.dto.events;

import ke.co.interviewusercaseworld.commons.dto.responses.LoanProductResponseDto;
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
    private String status;
    private String message;
    private LoanProductResponseDto productDetails; // todo: create a minimized object for this event
}
