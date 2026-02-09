package ke.co.expd.authserver.config;

import ke.co.expd.authserver.service.LoginAttemptsService;
import ke.co.expd.authserver.service.RateLimitService;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Component
@RequiredArgsConstructor
public class CustomReactiveAuthManager implements ReactiveAuthenticationManager {

    private final ReactiveUserDetailsService userDetailsService;
    private final LoginAttemptsService loginAttemptsService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.JWT_AUTHENTICATION, "Authenticating user", null);

        return rateLimitService.checkRateLimit(username, "/login")
                .flatMap(isAllowed -> {
                    return userDetailsService.findByUsername(username)
                            .defaultIfEmpty(CustomUserPrincipal.builder().build())
                            .switchIfEmpty(loginAttemptsService.recordFailedAttempt(username, "/login")
                                    .then(Mono.error(new BadCredentialsException("Invalid credentials"))))
                            .flatMap(userDetails -> {

                                System.out.println(userDetails.toString());

                                // Check if user is enabled
                                if (!userDetails.isEnabled()) {
                                    Helpers.log("", LogLevelEnum.DEBUG, OperationNameEnum.ACCOUNT_STATUS_VALIDATION, "User account is disabled", null);
                                    return Mono.error(new DisabledException("User account is disabled"));
                                }

                                // Check if account is locked
                                if (!userDetails.isAccountNonLocked()) {
                                    Helpers.log("", LogLevelEnum.DEBUG, OperationNameEnum.ACCOUNT_STATUS_VALIDATION, "User account is locked", null);
                                    return Mono.error(new LockedException("User account is locked"));
                                }

                                // Check if credentials are expired
                                if (!userDetails.isCredentialsNonExpired()) {
                                    Helpers.log("", LogLevelEnum.DEBUG, OperationNameEnum.ACCOUNT_STATUS_VALIDATION, "User credentials have expired", null);
                                    return Mono.error(new CredentialsExpiredException("User credentials have expired"));
                                }

                                // Verify password
                                System.out.println("Passwords match");
                                if (passwordEncoder.matches(password, userDetails.getPassword())) {
                                    Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.CREDENTIAL_VALIDATION, "Passwords match", null);
                                    // Password matches - clear failed attempts
                                    return loginAttemptsService.clearAttempts(username, "login")
                                            .then(Mono.fromCallable(() -> {
                                                // Create successful authentication
                                                Authentication auth = new UsernamePasswordAuthenticationToken(
                                                        userDetails,
                                                        password,
                                                        userDetails.getAuthorities()
                                                );

                                                log.info("User {} authenticated successfully", username);
                                                return auth;
                                            }));
                                } else {
                                    Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.CREDENTIAL_VALIDATION, "Passwords don't match", null);
                                    // Password doesn't match - record failed attempt
                                    return loginAttemptsService.recordFailedAttempt(username, "login")
                                            .then(Mono.error(new BadCredentialsException("Invalid credentials")));
                                }

                            });
                });
    }
}
