package ke.co.expd.authserver.config;

import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import ke.co.expd.authserver.model.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static ke.co.expd.authserver.service.impl.AuthServiceImpl.toUserResponseDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CustomUserPrincipal implements UserDetails, Principal {

    private UUID id;
    private String username;
    private String password;
    private String email;
    private boolean isEmailVerified;
    private boolean isEnabled;
    private boolean isCredentialsNonExpired;
    private boolean isAccountNonExpired;
    private boolean isAccountNonLocked;
    private UserResponseDto details;
    private List<String> roles;

    public static CustomUserPrincipal create(User user) {
        return CustomUserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .details(toUserResponseDto(user))
                .build();
    }

    @Override
    public String getName() {
        return this.username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role )).toList();
    }

    @Override
    public @Nullable String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.isAccountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.isAccountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.isCredentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }
}
