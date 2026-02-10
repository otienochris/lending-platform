package ke.co.interviewusercaseworld.commons.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentEvent {
    private String status;
    private String message;
    private UUID commandId;
    @Builder.Default
    private Boolean paymentSuccessful = false;
    @Builder.Default
    private Boolean isValidated = false;
}
