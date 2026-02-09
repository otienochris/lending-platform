package ke.co.expd.authserver.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(schema = "auth", name = "auth_users")
public class User {

    @Id
    private UUID id;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("is_enabled")
    private boolean isEnabled = true;

    @Column("is_email_verified")
    private boolean isEmailVerified = false;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    //    private boolean isEnabled;
    @Column("is_credentials_non_expired")
    private boolean isCredentialsNonExpired;

    @Column("is_account_non_expired")
    private boolean isAccountNonExpired;

    @Column("is_account_non_locked")
    private boolean isAccountNonLocked;

    @Transient
    private Set<Role> roles = new HashSet<>();
}
