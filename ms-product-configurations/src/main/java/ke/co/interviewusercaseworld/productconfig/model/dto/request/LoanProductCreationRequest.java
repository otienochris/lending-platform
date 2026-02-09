package ke.co.interviewusercaseworld.productconfig.model.dto.request;

import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureOptionsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanProductCreationRequest {
    private String productName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private InterestRateTypeEnum interestRateType;
    private List<Integer> tenureOptions;
    private TenureOptionsTypeEnum tenureOptionsType;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
