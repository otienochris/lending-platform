package ke.co.interviewusercaseworld.productconfig.model.dto.request;

import ke.co.interviewusercaseworld.commons.enums.FeeApplicableAtEnum;
import ke.co.interviewusercaseworld.commons.enums.FeeTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.FeeValueTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeeRequestDto {
    private FeeTypeEnum feeType;
    private BigDecimal feeValue;
    private FeeValueTypeEnum feeValueType;
    private FeeApplicableAtEnum applicableAt;

}
