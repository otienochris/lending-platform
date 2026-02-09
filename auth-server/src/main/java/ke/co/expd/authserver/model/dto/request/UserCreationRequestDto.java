package ke.co.expd.authserver.model.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreationRequestDto {
    private String username;
    @Email
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String msisdn;
}
