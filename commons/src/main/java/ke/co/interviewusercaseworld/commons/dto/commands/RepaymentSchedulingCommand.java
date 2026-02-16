package ke.co.interviewusercaseworld.commons.dto.commands;

import ke.co.interviewusercaseworld.commons.enums.InstallmenFrequencyEnum;
import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.RepaymentOptionEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureUnitEnum;
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
public class RepaymentSchedulingCommand {
    private UUID commandId;
    private UUID loanId;
    private UUID customerId;
    private UUID productId;
    private BigDecimal principal;
    private BigDecimal interestRate;
    @Builder.Default
    private InterestRateTypeEnum interestRateType = InterestRateTypeEnum.FlatRate;
    private Integer tenure;
    @Builder.Default
    private TenureUnitEnum tenureType = TenureUnitEnum.MONTHS;
    @Builder.Default
    private RepaymentOptionEnum repaymentOption = RepaymentOptionEnum.BULLET;
    @Builder.Default
    private InstallmenFrequencyEnum installmentFrequency = InstallmenFrequencyEnum.MONTHLY;

}
