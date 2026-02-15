package ke.co.interviewusercaseworld.commons.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductValidationOutcomeDto {
    private LoanProductResponseDto productDetails;
}
