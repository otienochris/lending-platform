package ke.co.interviewusercaseworld.msorchestrator.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SagaQueryResponse {
    private UUID id;

    private String sagaType;

    private String businessKey;

    private String status;

    private String currentStep;
    private Object originalRequest;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SagaStepQueryResponse> steps;
}
