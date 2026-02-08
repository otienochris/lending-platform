package ke.co.interviewusercaseworld.commons.dto.requests;

import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
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
    private OperationNameEnum operation;
    private String requestRefId;
}
