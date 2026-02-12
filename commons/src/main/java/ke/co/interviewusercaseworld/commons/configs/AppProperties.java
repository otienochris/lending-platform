package ke.co.interviewusercaseworld.commons.configs;

import ke.co.interviewusercaseworld.commons.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppProperties {

    private ProductConfigsProperties productConfiguration;
    private OrchestratorProperties orchestratorProperties;
    private AuthServerProperties authServerProperties;
    private LoanDisbursementProperties loanDisbursementProperties;
    private LoanRepaymentProperties loanRepaymentProperties;
    private NotificationProperties notificationProperties;
    private SchedulerProperties schedulerProperties;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SchedulerProperties {
        private SecurityConfigSpec securityConfigSpec;
        private Map<String, ServiceSetup> externalMicroServices;
        private KafkaConfigs kafkaConfigs;
        private JobRunnerConfig jobRunnerConfig;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobRunnerConfig {

        private DatasourceConfig datasource;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DatasourceConfig {

        private String url;
        private String username;
        private String password;
        private String driverClassName;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotificationProperties {
        private SecurityConfigSpec securityConfigSpec;
        private Map<String, ServiceSetup> externalMicroServices;
        private KafkaConfigs kafkaConfigs;
        private Map<NotificationTemplateEnum, Map<NotificationTypeEnum, TemplateDetails>> notificationTemplates;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TemplateDetails {

        private NotificationTemplateEnum name;
        private String subject;
        private String template;
        private String from;
        private Map<String, Object> paramValues;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoanRepaymentProperties {
        private SecurityConfigSpec securityConfigSpec;
        private Map<String, ServiceSetup> externalMicroServices;
        private KafkaConfigs kafkaConfigs;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoanDisbursementProperties {
        private SecurityConfigSpec securityConfigSpec;
        private Map<String, ServiceSetup> externalMicroServices;
        private KafkaConfigs kafkaConfigs;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthServerProperties {
        private SecurityConfigSpec securityConfigSpec;
        private Map<String, ServiceSetup> externalMicroServices;
        private KafkaConfigs kafkaConfigs;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrchestratorProperties {
        private SecurityConfigSpec securityConfigSpec;
        private Map<String, ServiceSetup> externalMicroServices;
        private KafkaConfigs kafkaConfigs;
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductConfigsProperties {

        private SecurityConfigSpec securityConfigSpec;
        private Map<String, ServiceSetup> externalMicroServices;
        private KafkaConfigs kafkaConfigs;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KafkaConfigs {

        private Map<CommandsEnum, String> topicsForCommand;

    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceSetup{
        private String name;
        private String baseUrl;
        private AuthApiSpec auth;
        private ApiSpec productValidationApiSpec;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApiSpec{
        private String url;
        private String version;
        private String description;
        private String contextPath;
        @Builder.Default
        private boolean isProtected = false;
        private Map<String, String> defaultHeaders;
        private Map<String, String> defaultRequestParams;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthApiSpec{
        private String url;
        private String version;
        private String description;
        private String contextPath;
        private String username;
        private String password;
        private String token;
        private AuthTypeEnum authType;
        private Map<String, String> defaultHeaders;
        private Map<String, String> defaultRequestParams;
    }


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
