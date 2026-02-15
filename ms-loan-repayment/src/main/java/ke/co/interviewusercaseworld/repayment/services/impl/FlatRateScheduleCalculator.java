package ke.co.interviewusercaseworld.repayment.services.impl;

import ke.co.interviewusercaseworld.commons.enums.InstallmenFrequencyEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.RepaymentOptionEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.RepaymentScheduleResponseDto;
import ke.co.interviewusercaseworld.repayment.services.ScheduleCalculator;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.*;

@Service
public class FlatRateScheduleCalculator implements ScheduleCalculator {

    private static final MathContext MC = new MathContext(12, RoundingMode.CEILING);

    public static @NonNull BigDecimal calculateInterestByInstallments(InstallmenFrequencyEnum frequency, BigDecimal annualRate, Integer installments, BigDecimal principal) {
        BigDecimal ratePerPeriod = ratePerPeriod(frequency, annualRate);

        return principal
                .multiply(ratePerPeriod)
                .multiply(BigDecimal.valueOf(installments), MC)
                .setScale(2, RoundingMode.CEILING);
    }

    @Override
    public Flux<RepaymentScheduleResponseDto> apply(LoanRepaymentSchedulingDto request) {
        Helpers.log("", LogLevelEnum.info, OperationNameEnum.FLAT_RATE_SCHEDULE_GENERATION, "Generating flat rate schedule...", null);

        List<RepaymentScheduleResponseDto> schedules = new ArrayList<>();

        BigDecimal principal = request.getPrincipal();
        int installments = RepaymentOptionEnum.INSTALLMENT.equals(request.getRepaymentOption()) ? calculateInstallments(request.getTenure(), request.getTenureUnit(), request.getInstallmentFrequency()) : 1;

        BigDecimal totalInterest = calculateInterestByInstallments(request.getInstallmentFrequency(), request.getAnnualInterestRate(), installments, principal);

        BigDecimal totalPayable = principal.add(totalInterest);

        BigDecimal installmentAmount = totalPayable.divide(BigDecimal.valueOf(installments), 2, RoundingMode.CEILING);

        BigDecimal principalPerInstallment = principal
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.CEILING);

        BigDecimal interestPerInstallment = totalInterest
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.CEILING);

        AtomicReference<BigDecimal> remaining = new AtomicReference<>(principal);

        return Flux.range(1, installments)
                .flatMap(i -> {
                    System.out.println("Installment: " + i);
                    remaining.set(remaining.get().subtract(principalPerInstallment));

                    RepaymentScheduleResponseDto schedule = RepaymentScheduleResponseDto.builder()
                            .installmentNumber(i)
                            .loanId(request.getLoanId())
                            .interestComponent(interestPerInstallment)
                            .status("PENDING")
                            .principalComponent(principalPerInstallment)
                            .emiAmount(installmentAmount)
                            .dueDate(calculateDueDate(request.getDisbursementDate(), request.getInstallmentFrequency(), i))
                            .remainingBalance(remaining.get().max(BigDecimal.ZERO))
                            .build();
                    return Mono.just(schedule);
                });
    }
}
