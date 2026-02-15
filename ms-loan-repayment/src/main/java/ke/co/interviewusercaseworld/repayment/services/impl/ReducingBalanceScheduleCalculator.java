package ke.co.interviewusercaseworld.repayment.services.impl;

import ke.co.interviewusercaseworld.commons.enums.InstallmenFrequencyEnum;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.RepaymentOptionEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.RepaymentScheduleResponseDto;
import ke.co.interviewusercaseworld.repayment.services.ScheduleCalculator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.*;

@Service
public class ReducingBalanceScheduleCalculator implements ScheduleCalculator {

    private static final MathContext MC = new MathContext(12, RoundingMode.CEILING);

    @Override
    public Flux<RepaymentScheduleResponseDto> apply(LoanRepaymentSchedulingDto request) {
        Helpers.log("", LogLevelEnum.info, OperationNameEnum.REDUCING_BALANCE_SCHEDULE_GENERATION, "Generating reducing balance schedule...", null);

        List<RepaymentScheduleResponseDto> schedules = new ArrayList<>();

        BigDecimal principal = request.getPrincipal();

        int installments = RepaymentOptionEnum.INSTALLMENT.equals(request.getRepaymentOption()) ? calculateInstallments(request.getTenure(), request.getTenureUnit(), request.getInstallmentFrequency()) : 1;

        InstallmenFrequencyEnum installmentFrequency = request.getInstallmentFrequency();

        BigDecimal ratePerPeriod = ratePerPeriod(installmentFrequency, request.getAnnualInterestRate());

        BigDecimal emi = calculateEmi(principal, ratePerPeriod, installments);

        final BigDecimal[] remaining = {principal};

        return Flux.range(1, installments)
                .flatMap(i -> {
                    BigDecimal interest = remaining[0].multiply(ratePerPeriod, MC);
                    BigDecimal principalRepaid = emi.subtract(interest);

                    remaining[0] = remaining[0].subtract(principalRepaid);

                    RepaymentScheduleResponseDto schedule = RepaymentScheduleResponseDto.builder()
                            .installmentNumber(i)
                            .loanId(request.getLoanId())
                            .interestComponent(interest.setScale(2, RoundingMode.CEILING))
                            .status("PENDING")
                            .principalComponent(principalRepaid.setScale(2, RoundingMode.CEILING))
                            .emiAmount(emi.setScale(2, RoundingMode.CEILING))
                            .dueDate(calculateDueDate(request.getDisbursementDate(), request.getInstallmentFrequency(), i))
                            .remainingBalance(remaining[0].max(BigDecimal.ZERO).setScale(2, RoundingMode.CEILING))
                            .build();
                    return Mono.just(schedule);
                });
    }
}
