package ke.co.interviewusercaseworld.commons.dto.commands;

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
public class UserValidationCommand {
    private UUID commandId;
    private UUID userId;
    private BigDecimal loanAmount;
}
