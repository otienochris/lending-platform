package ke.co.interviewusercaseworld.commons.utils;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
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
            case info,INFO-> log.info("Operation={} | RequestRefId={} | Message={} ", operation,requestRefId, message);
            case warn,WARN -> log.warn("Operation={} | RequestRefId={} | Message={} | ErrorMessage={} ", operation,requestRefId, message, e.getMessage());
            case error,ERROR -> log.error("Operation={} | RequestRefId={} | Message={} | ErrorMessage={} ", operation,requestRefId, message, e.getMessage());
            case trace,TRACE -> log.trace("Operation={} | RequestRefId={} | Message={} | ErrorMessage={} ", operation,requestRefId, message, e.getMessage());
            case debug,DEBUG -> log.debug("Operation={} | RequestRefId={} | Message={} | ErrorMessage={} ", operation,requestRefId, message, e.getMessage());
        }
    }
    /**
     * Get base URL from ServerHttpRequest
     */
    /*public static String getBaseUrl(ServerHttpRequest request) {

        String scheme = request.getSslInfo() != null ? "https" : "http";

        URI uri = request.getURI();

        String host = uri.getHost();
        int port = uri.getPort();

        // Handle default ports
        if (port == -1) {
            port = scheme.equals("https") ? 443 : 80;
        }

        // Build URL
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(host);

        // Only include port if it's not standard for the scheme
        if (!((scheme.equals("http") && port == 80) ||
                (scheme.equals("https") && port == 443))) {
            baseUrl.append(":").append(port);
        }

        return baseUrl.toString();
    }*/

    public static DefaultRequestHeader getDefaultRequestHeaderObject(Map<String, String> headers) {
        String correlationId = headers.getOrDefault("X-Correlation-ID", "");
        String requestRefId = headers.getOrDefault("X-Request-Ref-Id", "");
        String sourceSystem = headers.getOrDefault("X-Source-System", "");
        String operation = headers.getOrDefault("X-Operation", "");
        String token = headers.getOrDefault("Authorization", "");
        return DefaultRequestHeader.builder()
                .requestRefId(requestRefId)
                .correlationId(correlationId)
                .sourceSystem(sourceSystem)
                .operation(OperationNameEnum.valueOf(operation))
                .token(token)
                .build();
    }
}
