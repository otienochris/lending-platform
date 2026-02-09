package ke.co.interviewusercaseworld.commons.dto.responses;

import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DefaultResponseHeader {
    private UUID correlationId;
    private String sourceSystem;
    private OperationNameEnum operation;
    private String responseRefId;
    private ResponseCodes responseCode;
    private String customerMessage;
    private String debugMessage;
}
