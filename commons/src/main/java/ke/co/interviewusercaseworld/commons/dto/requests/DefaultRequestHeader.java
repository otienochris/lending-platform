package ke.co.interviewusercaseworld.commons.dto.requests;

import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DefaultRequestHeader {
    private UUID correlationId;
    private String sourceSystem;
    private OperationNameEnum operation;
    private String requestRefId;
    private String timestamp;
    private String token;
}
