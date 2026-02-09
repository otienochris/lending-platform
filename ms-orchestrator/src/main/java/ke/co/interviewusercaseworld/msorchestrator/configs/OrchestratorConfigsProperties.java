package ke.co.interviewusercaseworld.msorchestrator.configs;

import ke.co.interviewusercaseworld.commons.configs.AppProperties;
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
public class OrchestratorConfigsProperties {

    private AppProperties serviceProperties;
}
