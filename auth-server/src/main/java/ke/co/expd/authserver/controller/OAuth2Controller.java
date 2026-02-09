package ke.co.expd.authserver.controller;

import com.nimbusds.jose.KeySourceException;
import ke.co.expd.authserver.config.CustomUserPrincipal;
import ke.co.expd.authserver.config.JwtTokenProvider;
import ke.co.expd.authserver.model.dto.request.LoginRequest;
import ke.co.expd.authserver.model.dto.request.RefreshTokenRequest;
import ke.co.expd.authserver.model.dto.response.AuthResponse;
import ke.co.expd.authserver.model.dto.response.OAuth2TokenResponse;
import ke.co.expd.authserver.model.dto.response.UserValidationResponse;
import ke.co.expd.authserver.service.AuthService;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.LoginStrategyEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.getDefaultRequestHeaderObject;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
public class OAuth2Controller {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;


    // OAuth2 Authorization Endpoint (for authorization_code flow)
    @GetMapping("/oauth/authorize")
    public Mono<ResponseEntity<Void>> authorize(
            @RequestParam String response_type,
            @RequestParam String client_id,
            @RequestParam String redirect_uri,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String state,
            ServerWebExchange exchange,
            Authentication authentication
    ) {

        return authService.validateRequest(response_type, client_id, redirect_uri, scope, state, exchange)
                .flatMap(isValid -> {
                    if (isValid) {

                        if (authentication != null && authentication.isAuthenticated()) {
                            // User is authenticated, process authorization
                            return Mono.just(ResponseEntity.status(FOUND).location(URI.create(redirect_uri)).build());
                        } else {
                            return Mono.just(ResponseEntity.status(UNAUTHORIZED).build());
                        }
                    } else {
                        return Mono.just(ResponseEntity.status(UNAUTHORIZED).build());
                    }
                });
    }

    @GetMapping("/oauth/users/{userId}/validation")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, UserValidationResponse>>> token(@PathVariable UUID userId,
                                                                                                      @RequestHeader Map<String, String> headers){

        return authService.validateUser(userId, headers)
                .flatMap(res -> {
                    if (res.getHeader().getResponseCode().name().startsWith("RC_2")) {
                        return Mono.just(ResponseEntity.ok(res));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(res));
                    }
                }).onErrorResume(throwable -> {
                    return Mono.just(ResponseEntity.badRequest().body(GenericResponse.<DefaultResponseHeader, UserValidationResponse>builder()
                                    .header(DefaultResponseHeader.builder()
                                            .sourceSystem("USSD")
                                            .correlationId("")
                                            .responseCode(ResponseCodes.RC_500)
                                            .customerMessage("Error occurred while validating user")
                                            .debugMessage(throwable.getMessage())
                                            .responseRefId("")
                                            .build())
                            .build()));
                });
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<OAuth2TokenResponse>> token(
            @RequestParam(name = "grant_type") LoginStrategyEnum grantType,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String client_secret,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String refresh_token,
            @RequestParam(required = false) String scope,
            @RequestHeader(name = "x-request-ref-id", required = false) String requestRefId

    ) {
        System.out.println("username: " + username);
        Helpers.log(requestRefId, LogLevelEnum.INFO, OperationNameEnum.TOKEN_REQUEST_HANDLING, "Handling request for token: " + grantType.name(), null);
        return switch (grantType) {
            case password -> authService.login(new LoginRequest(username, password))
                    .map(this::mapToOAuth2TokenResponse);
            case refresh_token -> authService.refreshToken(new RefreshTokenRequest(refresh_token))
                    .map(this::mapToOAuth2TokenResponse);
            case client_credentials -> authService.clientCredentialsLogin(client_id, client_secret)
                    .map(this::mapToOAuth2TokenResponse);
            case authorization_code -> authService.exchangeCodeForToken(code, client_id, redirect_uri)
                    .map(this::mapToOAuth2TokenResponse);
        };

    }

    private ResponseEntity<OAuth2TokenResponse> mapToOAuth2TokenResponse(AuthResponse authResponse) {
        //todo: map to OAuth2TokenResponse
        return ResponseEntity.ok(OAuth2TokenResponse.builder()
                .scope("openid profile email roles")
                .accessToken(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .expiresIn(authResponse.getExpiresAt())
                .tokenType(authResponse.getTokenType())
                .build());
    }

    // OpenID Connect UserInfo Endpoint
    @GetMapping("/oauth/userinfo")
    public Mono<ResponseEntity<Map<String, Object>>> userinfo(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Map<String, Object> userInfo = Map.of(
                "sub", userPrincipal.getId().toString(),
                "preferred_username", userPrincipal.getUsername(),
                "email", userPrincipal.getEmail(),
                "email_verified", userPrincipal.isEmailVerified(),
                "given_name", userPrincipal.getDetails().getFirstName(),
                "family_name", userPrincipal.getDetails().getLastName(),
                "roles", userPrincipal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList())
        );

        return Mono.just(ResponseEntity.ok(userInfo));
    }

    // JWKS Endpoint (JSON Web Key Set)
    @GetMapping("/.well-known/jwks.json")
    public Mono<ResponseEntity<Map<String, Object>>> jwks() throws KeySourceException {
        System.out.println("getting jwks");
        Map<String, Object> jwks = tokenProvider.getJwks();
        return Mono.just(ResponseEntity.ok(jwks));
    }

    // OpenID Connect Discovery Endpoint
    @GetMapping("/.well-known/openid-configuration")
    public Mono<ResponseEntity<Map<String, Object>>> openIdConfiguration(ServerWebExchange exchange) {

        String host = getBaseUrl(exchange.getRequest());
        Map<String, Object> config = new java.util.HashMap<>(Map.of(
                "issuer", host,
                "authorization_endpoint", host + "/oauth/authorize",
                "token_endpoint", host + "/oauth/token",
                "userinfo_endpoint", host + "/oauth/userinfo",
                "jwks_uri", host + "/.well-known/jwks.json",
                "scopes_supported", List.of("openid", "profile", "email", "roles"),
                "subject_types_supported", List.of("public"),
                "response_types_supported", List.of("code", "token"),
                "grant_types_supported", List.of("authorization_code", "password", "client_credentials", "refresh_token"),
                "token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post")
        ));
        config.put("claims_supported", List.of("sub", "iss", "aud", "exp", "iat", "auth_time",
                "preferred_username", "email", "email_verified",
                "given_name", "family_name", "roles"
        ));

        return Mono.just(ResponseEntity.ok(config));
    }

    public static String getBaseUrl(ServerHttpRequest request) {


            String scheme = request.getSslInfo() != null ? "https" : "http";

            URI uri = request.getURI();

            String host = uri.getHost();
            int port = uri.getPort();

            // Handle default ports
            if (port == -1) {
                port = scheme.equals("https") ? 443 : 80;
            }

            // Build URL
            StringBuilder baseUrl = new StringBuilder();
            baseUrl.append(scheme).append("://").append(host);

            // Only include port if it's not standard for the scheme
            if (!((scheme.equals("http") && port == 80) ||
                    (scheme.equals("https") && port == 443))) {
                baseUrl.append(":").append(port);
            }

            return baseUrl.toString();

    }
}
