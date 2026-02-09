package ke.co.interviewusercaseworld.msorchestrator.model.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(schema = "orchestrator", name = "command_deduplication")
public class CommandDeDuplication {
    @Id
    private UUID commandId;
    private LocalDateTime processedAt;
}
