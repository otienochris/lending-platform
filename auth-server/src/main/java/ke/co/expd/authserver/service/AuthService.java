package ke.co.expd.authserver.service;

import ke.co.expd.authserver.model.dto.request.LoginRequest;
import ke.co.expd.authserver.model.dto.request.RefreshTokenRequest;
import ke.co.expd.authserver.model.dto.response.AuthResponse;
import ke.co.expd.authserver.model.dto.response.UserValidationResponse;
import ke.co.expd.authserver.model.entities.RefreshToken;
import ke.co.expd.authserver.model.dto.request.RegisterRequest;
import ke.co.expd.authserver.model.entities.User;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public interface AuthService {
    Mono<AuthResponse> login(LoginRequest request);
    Mono<AuthResponse> refreshToken(RefreshTokenRequest request);
    Mono<Void> logout(String refreshToken, ServerWebExchange exchange);
    Mono<User> register(RegisterRequest request);
    Mono<RefreshToken> createRefreshToken(UUID userId);
    Mono<RefreshToken> validateRefreshToken(String token);

    Mono<AuthResponse> clientCredentialsLogin(String clientId, String clientSecret);

    Mono<AuthResponse> exchangeCodeForToken(String code, String clientId, String redirectUri);

    Mono<Boolean> validateRequest(String responseType, String clientId, String redirectUri, String scope, String state, ServerWebExchange exchange);

    Mono<GenericResponse<DefaultResponseHeader, UserValidationResponse>> validateUser(UUID userId, Map<String, String> headers);
}
