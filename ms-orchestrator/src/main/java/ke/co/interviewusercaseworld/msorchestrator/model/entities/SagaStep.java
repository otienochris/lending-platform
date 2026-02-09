package ke.co.interviewusercaseworld.msorchestrator.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "orchestrator", name = "saga_steps")
public class SagaStep {
    @Id
    @Column("step_id")
    private UUID id;
    private UUID sagaId;
    private String stepName;
    private String status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime executedAt;

}
