package ke.co.interviewusercaseworld.commons.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DefaultRequestHeader {
    private String correlationId;
    private String sourceSystem;
    private String operation;
    private String requestRefId;
}
