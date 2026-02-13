package ke.co.interviewusercaseworld.scheduler.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(schema = "auth", name = "outbox_events")
public class AuthOutboxEvent {
    @Id
    @Column("event_id")
    private UUID id;
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    private String payload;
    @Column("published")
    private boolean isPublished;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
