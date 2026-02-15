package ke.co.interviewusercaseworld.commons.dto.responses;

import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureUnitEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanProductResponseDto {
    private UUID id;
    private String productName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private InterestRateTypeEnum interestRateType;
    private List<Integer> tenureOptions;
    private TenureUnitEnum tenureOptionsType;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String currency;
    private Boolean supportInstallments;
    @Builder.Default
    private List<ProductFeeResponseDto> fees = new ArrayList<>();
}
