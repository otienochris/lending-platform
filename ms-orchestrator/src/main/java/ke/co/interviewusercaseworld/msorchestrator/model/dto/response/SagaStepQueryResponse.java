package ke.co.interviewusercaseworld.msorchestrator.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SagaStepQueryResponse {
    private UUID id;
    private UUID sagaId;
    private String stepName;
    private String status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime executedAt;
    private Object outcome;
}
