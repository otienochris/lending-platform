package ke.co.expd.authserver.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(schema = "auth", name = "user_roles")
public class UserRole {

    @Id
    private UserRoleId id;
}
