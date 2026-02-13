package ke.co.interviewusercaseworld.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class Helpers {

    private static final ObjectMapper mapper = new ObjectMapper();

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


    public static Object parseToObject(String json) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON: {}", json, e);
            return null;
        }
    }

    public static DefaultRequestHeader getDefaultRequestHeaderObject(Map<String, String> headers) {
        String correlationId = headers.getOrDefault("X-Correlation-ID", "");
        UUID uuidCorrelation = null;
        if (!correlationId.isBlank()) {
            try{
                UUID.fromString(correlationId);
            } catch (Exception e) {
                Helpers.log("", LogLevelEnum.warn, OperationNameEnum.UNSPECIFIED, "Invalid correlation id", e);
            } finally {
                uuidCorrelation = UUID.randomUUID();
            }
        } else {
            uuidCorrelation = UUID.randomUUID();
        }
        String requestRefId = headers.getOrDefault("X-Request-Ref-Id", "");
        String sourceSystem = headers.getOrDefault("X-Source-System", "");
        String operation = headers.getOrDefault("X-Operation", "");
        String token = headers.getOrDefault("Authorization", "");
        return DefaultRequestHeader.builder()
                .requestRefId(requestRefId.isBlank() ? Objects.requireNonNull(uuidCorrelation).toString() : requestRefId)
                .correlationId(uuidCorrelation)
                .sourceSystem(sourceSystem)
                .operation(operation.isBlank() ? OperationNameEnum.UNSPECIFIED: OperationNameEnum.valueOf(operation))
                .token(token)
                .build();
    }


    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);


    /**
     * EMI formula (reducing balance)
     *
     * P = principal
     * r = annualInterestRate / 12 / 100
     * n = number of months
     *
     * EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
     * @param principal
     * @param annualRate
     * @param months
     * @return
     */

    public static BigDecimal calculateEmi(
            BigDecimal principal,
            BigDecimal annualRate,
            int months
    ) {
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(12), MC)
                .divide(BigDecimal.valueOf(100), MC);

        BigDecimal onePlusRPowerN =
                monthlyRate.add(BigDecimal.ONE).pow(months, MC);

        BigDecimal numerator =
                principal.multiply(monthlyRate).multiply(onePlusRPowerN);

        BigDecimal denominator =
                onePlusRPowerN.subtract(BigDecimal.ONE);

        return numerator
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
