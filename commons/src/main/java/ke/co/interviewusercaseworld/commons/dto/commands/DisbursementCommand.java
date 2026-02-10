package ke.co.interviewusercaseworld.commons.dto.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DisbursementCommand {
    private String commandId;
    private String loanId;
    private String customerId;
    private String amount;
    private String productId;
}
