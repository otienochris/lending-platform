package ke.co.interviewusercaseworld.msorchestrator.model.dto.request.commands;

import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureUnitEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentCommand {
    private String commandId;
    private String loanId;
    private String customerId;
    private String principal;
    private String interestRate;
    private InterestRateTypeEnum interestRateType;
    private Integer tenure;
    private TenureUnitEnum tenureType;

}
