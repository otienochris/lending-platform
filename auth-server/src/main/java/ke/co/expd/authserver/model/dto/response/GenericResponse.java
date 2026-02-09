package ke.co.expd.authserver.model.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponse<T> {
    private Header header;
    private T body;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Header {
        private String responseRefId;
        private String sourceSystem;
        private String status;
        private String customerMessage;
        private String debugMessage;
    }
}
