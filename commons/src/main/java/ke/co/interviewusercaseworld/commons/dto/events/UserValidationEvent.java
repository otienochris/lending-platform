package ke.co.interviewusercaseworld.commons.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserValidationEvent {
    private UUID commandId;
    private UUID userId;
}
