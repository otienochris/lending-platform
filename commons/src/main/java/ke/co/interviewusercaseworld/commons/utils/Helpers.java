package ke.co.interviewusercaseworld.commons.utils;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Helpers {

    public static void log(String requestRefId, LogLevelEnum level, OperationNameEnum operation, String message,  Exception e) {

        if (e == null) {
            e = new Exception();
        }

        if (level == null) {
            level = LogLevelEnum.info;
        }

        if (operation == null) {
            operation = OperationNameEnum.UNSPECIFIED;
        }

        switch (level) {
            case info -> log.info("RequestRefId={} Operation={} Message={}", requestRefId, operation, message);
            case warn -> log.warn("RequestRefId={} Operation={} Message={} ErrorMessage={}", requestRefId, operation, message, e.getMessage());
            case error -> log.error("RequestRefId={} Operation={} Message={} ErrorMessage={}", requestRefId, operation, message, e.getMessage());
            case trace -> log.trace("RequestRefId={} Operation={} Message={} ErrorMessage={}", requestRefId, operation, message, e.getMessage());
            case debug -> log.debug("RequestRefId={} Operation={} Message={} ErrorMessage={}", requestRefId, operation, message, e.getMessage());
        }
    }

    public static DefaultRequestHeader getDefaultRequestHeaderObject(Map<String, String> headers) {
        String correlationId = headers.getOrDefault("X-Correlation-ID", "");
        String requestRefId = headers.getOrDefault("X-Request-Ref-Id", "");
        String sourceSystem = headers.getOrDefault("X-Source-System", "");
        String operation = headers.getOrDefault("X-Operation", "");
        return DefaultRequestHeader.builder()
                .requestRefId(requestRefId)
                .correlationId(correlationId)
                .sourceSystem(sourceSystem)
                .operation(OperationNameEnum.valueOf(operation))
                .build();
    }
}
