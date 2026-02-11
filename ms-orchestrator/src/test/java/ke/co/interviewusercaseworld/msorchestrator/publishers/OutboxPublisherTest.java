package ke.co.interviewusercaseworld.msorchestrator.publishers;

import ke.co.interviewusercaseworld.commons.configs.AppProperties;
import ke.co.interviewusercaseworld.commons.enums.CommandsEnum;
import ke.co.interviewusercaseworld.msorchestrator.configs.OrchestratorConfigsProperties;
import ke.co.interviewusercaseworld.msorchestrator.model.entities.OutBoxEvent;
import ke.co.interviewusercaseworld.msorchestrator.repository.OutBoxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutBoxEventRepository outboxRepo;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OrchestratorConfigsProperties appProperties;

    @InjectMocks
    private OutboxPublisher outboxPublisher;


    @BeforeEach
    void setup() {



        /*when(appProperties.getServiceProperties().getOrchestratorProperties()).thenReturn(AppProperties.OrchestratorProperties.builder()
                        .kafkaConfigs(AppProperties.KafkaConfigs.builder().topicsForCommand(topicMap).build())
                        .securityConfigSpec(AppProperties.SecurityConfigSpec.builder().build())
                .build());*/
    }

    @Test
    void shouldPublishOutboxEventAndMarkAsPublished() {
        OutBoxEvent event = OutBoxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId("loan-123")
                .eventType(CommandsEnum.PRODUCT_VALIDATION_COMMAND.name())
                .payload("{\"test\":\"data\"}")
                .isPublished(false)
                .build();

        Map<CommandsEnum, String> topicMap = Map.of(
                CommandsEnum.PRODUCT_VALIDATION_COMMAND, "product.validation.topic"
        );
        when(appProperties.getServiceProperties()).thenReturn(AppProperties.builder().orchestratorProperties(AppProperties.OrchestratorProperties.builder().kafkaConfigs(AppProperties.KafkaConfigs.builder().topicsForCommand(topicMap).build()).build()).build());

        when(outboxRepo.findTop20ByIsPublishedFalse())
                .thenReturn(Flux.just(event));

        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(future);

        when(outboxRepo.save(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // when
        outboxPublisher.publishOutboxEvent();

        // then
        verify(kafkaTemplate).send(
                eq("product.validation.topic"),
                eq("loan-123"),
                eq("{\"test\":\"data\"}")
        );

        ArgumentCaptor<OutBoxEvent> captor = ArgumentCaptor.forClass(OutBoxEvent.class);
        verify(outboxRepo).save(captor.capture());

        assertTrue(captor.getValue().isPublished());
    }


    @Test
    void shouldSkipPublishingWhenTopicIsMissing() {
        OutBoxEvent event = OutBoxEvent.builder()
                .aggregateId("loan-456")
                .eventType("UNKNOWN_COMMAND")
                .payload("{}")
                .isPublished(false)
                .build();

        when(outboxRepo.findTop20ByIsPublishedFalse())
                .thenReturn(Flux.just(event));

        outboxPublisher.publishOutboxEvent();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepo, never()).save(any());
    }

    @Test
    void shouldDoNothingWhenNoOutboxEventsExist() {
        when(outboxRepo.findTop20ByIsPublishedFalse())
                .thenReturn(Flux.empty());

        outboxPublisher.publishOutboxEvent();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepo, never()).save(any());
    }




}