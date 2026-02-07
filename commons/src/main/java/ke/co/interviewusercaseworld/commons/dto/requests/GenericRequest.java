package ke.co.interviewusercaseworld.commons.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenericRequest<B,H> {
    private B body;
    private H header;
}
