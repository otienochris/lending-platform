package ke.co.interviewusercaseworld.commons.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DefaultResponseHeader {
    private String correlationId;
    private String sourceSystem;
    private String operation;
    private String responseRefId;
}
