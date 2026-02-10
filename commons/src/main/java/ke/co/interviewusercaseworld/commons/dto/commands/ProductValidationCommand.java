package ke.co.interviewusercaseworld.commons.dto.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductValidationCommand {
    private UUID commandId;
    private UUID productId;
}
