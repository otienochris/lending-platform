package ke.co.interviewusercaseworld.productconfig.model.dto.response;

import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureOptionsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanProductCreationResponseDto {
    private UUID id;
    private String productName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private InterestRateTypeEnum interestRateType;
    private List<Integer> tenureOptions;
    private TenureOptionsTypeEnum tenureOptionsType;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String currency;
    private Boolean supportInstallments;
}
