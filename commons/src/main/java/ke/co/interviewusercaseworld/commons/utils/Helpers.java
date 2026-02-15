package ke.co.interviewusercaseworld.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.enums.InstallmenFrequencyEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureUnitEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

    private static final MathContext MC = new MathContext(15, RoundingMode.CEILING);

    public static @NonNull String convertFirstCharToLowerCase(String key) {
        StringBuilder sb = new StringBuilder(key);
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    // Convert annual rate into per-period rate

    public static LocalDateTime calculateDueDate(LocalDateTime start,
                                                 InstallmenFrequencyEnum frequency,
                                                 int installmentNumber) {

        return switch (frequency) {
            case DAILY -> start.plusDays(installmentNumber);
            case WEEKLY -> start.plusWeeks(installmentNumber);
            case MONTHLY -> start.plusMonths(installmentNumber);
            case YEARLY -> start.plusYears(installmentNumber);
        };
    }

    /**
     * Rate per period=Annual Interest Rate / Number of periods in a year
     *
     * @param frequency          - installment frequency
     * @param annualInterestRate - annual interest rate
     * @return rate per period
     */
    public static BigDecimal ratePerPeriod(InstallmenFrequencyEnum frequency, BigDecimal annualInterestRate) {

        BigDecimal annualRate = annualInterestRate.divide(BigDecimal.valueOf(100), MC);

        int periodsPerYear = switch (frequency) {
            case DAILY -> 365;
            case WEEKLY -> 52;
            case MONTHLY -> 12;
            case YEARLY -> 1;
        };

        return annualRate.divide(BigDecimal.valueOf(periodsPerYear), MC);
    }

    // EMI Formula
    public static BigDecimal calculateEMI(BigDecimal principal, BigDecimal interestRate, int installment) {

        if (interestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(installment), 2, RoundingMode.CEILING);
        }

        BigDecimal onePlusRPowerN =
                BigDecimal.ONE.add(interestRate).pow(installment, MC);

        BigDecimal numerator = principal.multiply(interestRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.CEILING);
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

    public static int calculateInstallments(Integer tenure, TenureUnitEnum tenureUnit, InstallmenFrequencyEnum installmentFrequency) {

        int totalDays = switch (tenureUnit) {
            case DAYS -> tenure;
            case WEEKS -> tenure * 7;
            case MONTHS -> tenure * 30;
            case YEARS -> tenure * 365;
        };

        return switch (installmentFrequency) {
            case DAILY -> totalDays;
            case WEEKLY -> totalDays / 7;
            case MONTHLY -> totalDays / 30;
            case YEARLY -> totalDays / 365;
        };
    }

    /**
     * EMI formula (reducing balance)
     *
     * P = principal
     * r = annualInterestRate / 12 / 100
     * n = number of months
     *
     * EMI = (P * r * (1 + r)^n) / ((1 + r)^n - 1)
     * @param principal - principal amount
     * @param ratePerPeriod - rate per period
     * @param installments - number of installments
     * @return
     */

    public static BigDecimal calculateEmi(
            BigDecimal principal,
            BigDecimal ratePerPeriod,
            int installments
    ) {

        BigDecimal onePlusRPowerN =
                ratePerPeriod.add(BigDecimal.ONE).pow(installments, MC);

        BigDecimal numerator =
                principal.multiply(ratePerPeriod).multiply(onePlusRPowerN);

        BigDecimal denominator =
                onePlusRPowerN.subtract(BigDecimal.ONE);

        return numerator
                .divide(denominator, 2, RoundingMode.CEILING);
    }
}
