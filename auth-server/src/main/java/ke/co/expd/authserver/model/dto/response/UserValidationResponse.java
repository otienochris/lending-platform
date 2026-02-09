package ke.co.expd.authserver.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
