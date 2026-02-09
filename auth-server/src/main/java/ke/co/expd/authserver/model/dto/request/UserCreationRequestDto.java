package ke.co.expd.authserver.model.dto.request;

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
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}
