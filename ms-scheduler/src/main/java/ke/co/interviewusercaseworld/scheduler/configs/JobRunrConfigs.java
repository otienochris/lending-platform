package ke.co.interviewusercaseworld.scheduler.configs;

import com.zaxxer.hikari.HikariDataSource;
import ke.co.interviewusercaseworld.commons.configs.AppProperties;
import lombok.RequiredArgsConstructor;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;


@Configuration
@RequiredArgsConstructor
public class JobRunrConfigs {

    private final SchedulerConfigsProperties appProperties;

    /*@Bean
    public PostgresStorageProvider postgresStorageProvider(DataSource dataSource) {
        return new PostgresStorageProvider(dataSource, "scheduler", StorageProviderUtils.DatabaseOptions.CREATE);
    }*/

    @Bean(name = "jobrunrDataSource")
    public DataSource jobrunrDataSource() {
        AppProperties.DatasourceConfig datasource = appProperties.getServiceProperties().getSchedulerProperties().getJobRunnerConfig().getDatasource();
        return DataSourceBuilder.create()
                .url(datasource.getUrl())
                .username(datasource.getUsername())
                .password(datasource.getPassword())
                .driverClassName(datasource.getDriverClassName())
                .type(HikariDataSource.class)
                .build();
    }

}
