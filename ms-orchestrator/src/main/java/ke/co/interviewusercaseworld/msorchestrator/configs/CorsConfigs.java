package ke.co.interviewusercaseworld.msorchestrator.configs;

import ke.co.interviewusercaseworld.commons.enums.HttpVerbEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * @author christopherochiengotieno@gmail.com
 * @version 1.0.0
 * @since Monday, 08/02/2026
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class CorsConfigs {

    private final OrchestratorConfigsProperties appProperties;

    private static String[] getAllowedMethods(HttpVerbEnum[] allowedMethods) {
        String[] tmp = new String[allowedMethods.length];
        for (int i = 0; i < allowedMethods.length; i++) {
            tmp[i] = allowedMethods[i].name();
        }
        return tmp;
    }

    @Bean
    public WebFluxConfigurer corsConfiguration() {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(org.springframework.web.reactive.config.CorsRegistry registry) {
                appProperties.getServiceProperties().getOrchestratorProperties().getSecurityConfigSpec().getCorsConfigs().forEach(corsConfig -> {
                    String mapping = corsConfig.getMapping();
                    Helpers.log(null, LogLevelEnum.info, OperationNameEnum.CORS_CONFIGURATION, "Configuring CORS for " + mapping, null);
                    registry.addMapping(mapping)
                            .allowedOrigins(corsConfig.getAllowedOrigins())
                            .allowedMethods(getAllowedMethods(corsConfig.getAllowedMethods()))
                            .allowedHeaders(corsConfig.getAllowedHeaders())
                            .allowCredentials(corsConfig.isAllowCredentials())
                            .maxAge(corsConfig.getMaxAge());
                });
            }
        };
    }

}
