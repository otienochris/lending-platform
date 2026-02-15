package ke.co.interviewusercaseworld.commons.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductFeeResponseDto {
    private String feeId;
    private String feeType;
    private BigDecimal feeValue;
    private String feeValueType;
    private String applicableAt;
}
