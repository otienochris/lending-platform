package ke.co.interviewusercaseworld.repayment.configs;


import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.*;

@Configuration
@RequiredArgsConstructor
public class KafkaConfigs {
    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    private final RepaymentConfigsProperties appProperties;


    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }


    @Bean
    public KafkaAdmin.NewTopics createMultipleTopics() {
        Map<CommandsEnum, String> topicsForCommand = appProperties.getServiceProperties().getLoanRepaymentProperties().getKafkaConfigs().getTopicsForCommand();

        List<String> topicNames = new ArrayList<>();

        String ref = UUID.randomUUID().toString();
        topicsForCommand.forEach((command, topic) -> {
            Helpers.log(ref, LogLevelEnum.info, OperationNameEnum.KAFKA_TOPIC_CONFIGURATION, "Creating topic " + topic, null);
            topicNames.add(topic);
        });


        NewTopic[] topics = topicNames.stream()
                .map(name -> TopicBuilder.name(name)
                        .partitions(3)
                        .replicas(1)
                        .build())
                .toArray(NewTopic[]::new);

        return new KafkaAdmin.NewTopics(topics);
    }



}
