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
public class UserValidationResponse {

    private boolean isValid;
    private String msisdn;
    private String email;
    private String lastName;
    private boolean isFraudulent;
    private boolean isBlacklisted;
    private BigDecimal loanLimit;
}
