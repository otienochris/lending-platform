package ke.co.interviewusercaseworld.commons.configs;

import ke.co.interviewusercaseworld.commons.enums.HttpVerbEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppProperties {

    private ProductConfigsProperties productConfiguration;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductConfigsProperties {


        private SecurityConfigSpec securityConfigSpec;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class SecurityConfigSpec {

            private List<CorsConfig> corsConfigs;
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
                private String principalAttribute;
                private String resourceId;
                private String issuerUri;
                private String jwkSetUri;
            }

            @Data
            @Builder
            @AllArgsConstructor
            @NoArgsConstructor
            public static class CorsConfig {
                private String mapping;
                private String[] allowedOrigins;
                private HttpVerbEnum[] allowedMethods;
                private String[] allowedHeaders;
                private boolean allowCredentials;
                private int maxAge;
            }
        }
    }




}
