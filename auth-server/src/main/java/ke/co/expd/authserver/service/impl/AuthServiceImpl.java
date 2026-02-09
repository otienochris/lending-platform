package ke.co.expd.authserver.service.impl;

import io.jsonwebtoken.Claims;
import ke.co.expd.authserver.config.CustomUserPrincipal;
import ke.co.expd.authserver.config.JwtTokenProvider;
import ke.co.expd.authserver.exceptions.InvalidTokenException;
import ke.co.expd.authserver.model.dto.request.LoginRequest;
import ke.co.expd.authserver.model.dto.request.RefreshTokenRequest;
import ke.co.expd.authserver.model.dto.request.RegisterRequest;
import ke.co.expd.authserver.model.dto.response.AuthResponse;
import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import ke.co.expd.authserver.model.entities.*;
import ke.co.expd.authserver.repoisitory.RefreshTokenRepository;
import ke.co.expd.authserver.repoisitory.RoleRepository;
import ke.co.expd.authserver.repoisitory.UserRepository;
import ke.co.expd.authserver.repoisitory.UserRoleRepository;
import ke.co.expd.authserver.service.AuthService;
import ke.co.expd.authserver.service.RateLimitService;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    @Qualifier("customReactiveAuthManager")
    private final ReactiveAuthenticationManager authenticationManager;
    private final RateLimitService rateLimitService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public static UserResponseDto toUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

        return authenticationManager.authenticate(usernamePasswordAuthenticationToken)
                .onErrorResume(throwable -> Mono.empty())
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid credentials")))
                .flatMap(authentication -> {
                    System.out.println("Auth ----");
                    if (!authentication.isAuthenticated()) {
                        Helpers.log("", LogLevelEnum.DEBUG, OperationNameEnum.ACCOUNT_STATUS_VALIDATION, "User account is locked", null);
                        return Mono.error(new BadCredentialsException("Invalid credentials"));
                    } else {
                        Helpers.log("", LogLevelEnum.DEBUG, OperationNameEnum.ACCOUNT_STATUS_VALIDATION, "User authenticated success", null);
                    }

                    CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();

                    return updateLastLogin(userPrincipal.getId())
                            .flatMap(savedUser -> Mono.zip(
                                    Mono.just(tokenProvider.generateAccessToken(authentication)),
                                    createRefreshToken(userPrincipal.getId()),
                                    Mono.just(toUserResponseDto(savedUser))))
                            .flatMap(tokens -> {
                                AuthResponse authResponse = AuthResponse.builder()
                                        .accessToken(tokens.getT1())
                                        .refreshToken(tokens.getT2().getToken())
                                        .userInfo(tokens.getT3())
                                        .build();
                                return tokenProvider.getAccessTokenValidity(tokens.getT1())
                                        .flatMap(instant -> {
                                            LocalDateTime expiresAt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                                            authResponse.setExpiresAt(expiresAt);
                                            return Mono.just(authResponse);
                                        });
                            });

                });
    }

    private Mono<User> updateLastLogin(UUID uuid) {
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.ACCOUNT_UPDATE, "updating last login", null);
        return userRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    user.setLastLoginAt(now());
                    Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.ACCOUNT_UPDATE, "updated last login", null);
                    return userRepository.save(user);
                });
    }

    @Override
    public Mono<AuthResponse> refreshToken(RefreshTokenRequest request) {
        return validateRefreshToken(request.getRefreshToken())
                .flatMap(refreshToken -> {
                    if (refreshToken.isRevoked()) {
                        return Mono.error(new InvalidTokenException("Refresh token was revoked"));
                    }

                    if (refreshToken.getExpiresAt().isBefore(now())) {
                        return Mono.error(new InvalidTokenException("Refresh token expired"));
                    }

                    return userRepository.findById(refreshToken.getUserId())
                            .flatMap(user -> {
                                CustomUserPrincipal userPrincipal = CustomUserPrincipal.create(user);
                                String accessToken = tokenProvider.generateAccessToken(new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));

                                return createRefreshToken(user.getId())
                                        .flatMap(newRefreshToken ->
                                                {
                                                    return tokenProvider.getAccessTokenValidity(accessToken).flatMap(instant -> {
                                                        AuthResponse authResponse = AuthResponse.builder()
                                                                .accessToken(accessToken)
                                                                .refreshToken(newRefreshToken.getToken())
                                                                .expiresAt(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                                                                .userInfo(toUserResponseDto(user))
                                                                .build();
                                                        return Mono.just(authResponse);
                                                    });
                                                }
                                        );
                            });
                });
    }

    @Override
    public Mono<Void> logout(String refreshToken, ServerWebExchange exchange) {
        return validateRefreshToken(refreshToken)
                .flatMap(token -> {
                    token.setRevoked(true);
                    return refreshTokenRepository.save(token);
                })
                .then(Mono.fromRunnable(() -> {
                    // Clear authentication context
                    exchange.getAttributes().remove(SecurityContext.class.getName());
                }));
    }

    @Override
    public Mono<User> register(RegisterRequest request) {
        return validateUsernameAndEmail(request)
                .flatMap(usernameOrEmailExists -> {
                    User user = User.builder()
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .isEmailVerified(false)
                            .isEnabled(true)
                            .createdAt(now())
                            .lastLoginAt(now())
                            .firstName(request.getUsername())
                            .lastName(request.getUsername())
                            .build();
                    return userRepository.save(user)
                            .flatMap(savedUser -> roleRepository.findByName("ROLE_USER")
                                    .flatMap(role -> {
                                        UserRole userRole = new UserRole(new UserRoleId(savedUser.getId(), role.getId()));
                                        return userRoleRepository.save(userRole);
                                    })
                                    .thenReturn(savedUser));

                });
    }

    private @NonNull Mono<Boolean> validateUsernameAndEmail(RegisterRequest request) {
        return userRepository.existsByUsername(request.getUsername())
                .defaultIfEmpty(false)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Username is already taken"));
                    }

                    return userRepository.existsByEmail(request.getEmail())
                            .defaultIfEmpty(false)
                            .flatMap(emailExists -> {
                                if (emailExists) {
                                    return Mono.error(new RuntimeException("Email is already in use"));
                                }
                                return Mono.just(emailExists);
                            });
                });
    }

    @Override
    public Mono<RefreshToken> createRefreshToken(UUID userId) {

        RefreshToken refreshToken = RefreshToken.builder()
                .isRevoked(false)
                .userId(userId)
                .token(tokenProvider.generateRefreshToken(userId))
                .expiresAt(now().plusHours(1))
                .createdAt(now())
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Mono<RefreshToken> validateRefreshToken(String token) {
        try {
            Claims claims = tokenProvider.getJwtParser().parseSignedClaims(token).getPayload();
            if (!"refresh".equals(claims.get("type", String.class))) {
                return Mono.error(new InvalidTokenException("Not a refresh token"));
            }

            String tokenId = claims.get("token", String.class);
            return refreshTokenRepository.findByToken(tokenId)
                    .switchIfEmpty(Mono.error(new InvalidTokenException("Refresh token not found")));
        } catch (Exception e) {
            return Mono.error(new InvalidTokenException("Invalid token type: " + e.getMessage()));
        }

    }

    @Override
    public Mono<AuthResponse> clientCredentialsLogin(String clientId, String clientSecret) {
        return Mono.error(new RuntimeException("Not implemented yet"));
    }

    @Override
    public Mono<AuthResponse> exchangeCodeForToken(String code, String clientId, String redirectUri) {
        return Mono.error(new RuntimeException("Not implemented yet"));
    }

    @Override
    public Mono<Boolean> validateRequest(String responseType, String clientId, String redirectUri, String scope, String state, ServerWebExchange exchange) {
        return Mono.just(true);
    }
}
