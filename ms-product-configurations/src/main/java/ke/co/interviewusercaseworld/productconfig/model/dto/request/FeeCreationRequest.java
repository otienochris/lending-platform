package ke.co.interviewusercaseworld.productconfig.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeeCreationRequest {
    private String feeType;
    private BigDecimal feeAmount;
}
