package ke.co.expd.authserver.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(schema = "auth", name = "roles")
public class Role {
    @Id
    private UUID id;

    private String name;
    private String description;

    @Transient
    private Set<Permission> permissions = new HashSet<>();
}
