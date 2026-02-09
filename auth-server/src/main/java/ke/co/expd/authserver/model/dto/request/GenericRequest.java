package ke.co.expd.authserver.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenericRequest<T> {
    private Header header;
    private T body;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Header {
        private String sourceSystem;
        private String requestRefId;
        private String operationName;
        private LocalDateTime timestamp;
    }
}
