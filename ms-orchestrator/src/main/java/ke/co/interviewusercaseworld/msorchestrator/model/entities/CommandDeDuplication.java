package ke.co.interviewusercaseworld.msorchestrator.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(schema = "orchestrator", name = "command_deduplication")
public class CommandDeDuplication {
    @Id
    private UUID commandId;
    private LocalDateTime processedAt;
}
