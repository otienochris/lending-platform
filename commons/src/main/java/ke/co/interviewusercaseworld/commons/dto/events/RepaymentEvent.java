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
public class RepaymentEvent {
    private UUID commandId;
    private String status;
    private String message;
    private String referenceNumber;
    private BigDecimal totalLoanAmount;
    private BigDecimal totalInterest;
    private String scheduleType;
    private LocalDateTime dueDate;
}
