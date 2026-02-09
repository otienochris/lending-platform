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
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Table(schema = "orchestrator", name = "sagas")
public class Saga {

    @Id
    @Column("saga_id")
    private UUID id;

    private String sagaType;

    private String businessKey;

    private String status;

    private String currentStep;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
