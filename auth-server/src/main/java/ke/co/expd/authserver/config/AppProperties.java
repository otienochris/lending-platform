package ke.co.expd.authserver.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private SecurityConfigSpec securityConfigSpec;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SecurityConfigSpec {

        private String[] whitelistedPaths;
        private JwtSpec jwtSpec;
        private PasswordSpec passwordSpec;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class PasswordSpec {
            @Builder.Default
            private int passwordLength = 12;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class JwtSpec {
            private String secret;
            private Long expiration;
            private Long refreshExpiration;
            private String issuer;
            private String keyId;
        }
    }
}
