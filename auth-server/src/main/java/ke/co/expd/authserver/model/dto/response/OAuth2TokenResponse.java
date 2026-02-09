package ke.co.expd.authserver.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2TokenResponse {
    private String accessToken;
    private String tokenType;
    private LocalDateTime expiresIn;
    private String scope;
    private String refreshToken;

}
