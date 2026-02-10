package ke.co.interviewusercaseworld.commons.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DisbursementEvent {
    private UUID commandId;
    private String status;
    private String message;

    private BigDecimal totalAmountToBePaid;
    private Integer installments;
    private LocalDateTime dueDate;
}
