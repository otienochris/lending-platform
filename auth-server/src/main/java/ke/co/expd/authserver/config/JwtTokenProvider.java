package ke.co.expd.authserver.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
@Getter
public class JwtTokenProvider {
    private final JwtParser jwtParser;
    private final SecretKey secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;
    private final String issuer;
    private final String secret;
    private final AppProperties appProperties;
    private final JwtEncoder jwtEncoder;
    private final ReactiveJwtDecoder reactiveJwtDecoder;
    private final JWKSource<SecurityContext> jwkSource;


    public JwtTokenProvider(AppProperties appProperties, JwtEncoder jwtEncoder, JWKSource<SecurityContext> jwkSource, ReactiveJwtDecoder reactiveJwtDecoder) {
        AppProperties.SecurityConfigSpec securityConfigSpec = appProperties.getSecurityConfigSpec();

        this.appProperties = appProperties;
        this.jwtEncoder = jwtEncoder;
        this.jwkSource = jwkSource;
        this.reactiveJwtDecoder = reactiveJwtDecoder;

        this.secret = securityConfigSpec.getJwtSpec().getSecret();
        this.secretKey = Keys.hmacShaKeyFor(this.secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = securityConfigSpec.getJwtSpec().getIssuer();
        this.accessTokenValidity = Duration.ofSeconds(securityConfigSpec.getJwtSpec().getExpiration());
        this.refreshTokenValidity = Duration.ofSeconds(securityConfigSpec.getJwtSpec().getRefreshExpiration());

        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build();
    }

    public String generateAccessToken(Authentication authentication) {

        org.springframework.security.oauth2.jwt.JwsHeader jwsHeader = org.springframework.security.oauth2.jwt.JwsHeader
                .with(SignatureAlgorithm.RS256)
                .type( "JWT")
                .algorithm(SignatureAlgorithm.RS256)
                .header("use", "sig")
                .build();

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        Instant now = Instant.now();
        Instant validity = now.plus(accessTokenValidity);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(principal.getUsername())
                .claim("id", principal.getId())
                .claim("preferred_username", principal.getUsername())
                .claim("roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .issuer(issuer)
                .issuedAt(Date.from(now).toInstant())
                .expiresAt(Date.from(validity).toInstant())
                .build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant validity = now.plus(refreshTokenValidity);

        String token = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .claim("token", token)
                .issuedAt(Date.from(now))
                .expiration(Date.from(validity))
                .issuer(issuer)
                .signWith(secretKey)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        Collection<? extends GrantedAuthority> authorities = extractAuthorities(claims);

        Principal principal = CustomUserPrincipal.builder()
                .username(claims.getSubject())
                .email(claims.get("email", String.class))
                .details((UserResponseDto) claims.get("details"))
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            Helpers.log("", LogLevelEnum.ERROR, OperationNameEnum.JWT_AUTHENTICATION, "Invalid JWT token: {}", e);
            return false;
        }
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(Claims claims) {
        List<String> roles = claims.get("roles", List.class);
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public Mono<Instant> getAccessTokenValidity(String token) {

        return reactiveJwtDecoder.decode(token)
                .flatMap(jwt -> {
                    return Mono.just(jwt.getExpiresAt());
                });
    }

    /**
     * Generate JWKS (JSON Web Key Set) for OAuth2 compliance
     * This is used by the /.well-known/jwks.json endpoint
     */
    public Map<String, Object> getJwks() {

        JWK rsaKey = getRsaKey();
        if (rsaKey == null) {
            return Map.of();
        }
        JWKSet jwkSet = new JWKSet(rsaKey);

        return jwkSet.toPublicJWKSet().toJSONObject();
    }

    private JWK getRsaKey() {

        try {

            // Try to get using a null selector (gets all keys)
            JWKMatcher.Builder matcher = new JWKMatcher.Builder();
            List<JWK> jwks = jwkSource.get(new JWKSelector(matcher.build()), null);
            if (jwks != null && !jwks.isEmpty()) {
                return jwks.getFirst();
            }
        } catch (Exception e) {
            // Log and fall through
            log.warn("Could not extract JWKSet directly: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Generate RSA key pair for asymmetric signing
     */
    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048); // 2048-bit RSA key
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

}
