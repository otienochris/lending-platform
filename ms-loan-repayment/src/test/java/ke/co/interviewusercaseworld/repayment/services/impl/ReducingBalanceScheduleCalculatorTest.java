package ke.co.interviewusercaseworld.repayment.services.impl;

import ke.co.interviewusercaseworld.commons.enums.InstallmenFrequencyEnum;
import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.RepaymentOptionEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureUnitEnum;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ReducingBalanceScheduleCalculatorTest {

    LoanRepaymentSchedulingDto monthlyInstallmentSchedulingDto;
    @InjectMocks
    private ReducingBalanceScheduleCalculator calculator;

    @BeforeEach
    void setUp() {

        monthlyInstallmentSchedulingDto = LoanRepaymentSchedulingDto.builder()
                .annualInterestRate(BigDecimal.valueOf(3))
                .principal(BigDecimal.valueOf(500))
                .tenure(3)
                .interestRateType(InterestRateTypeEnum.ReducingBalance)
                .disbursementDate(LocalDateTime.now())
                .repaymentOption(RepaymentOptionEnum.INSTALLMENT)
                .loanId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .installmentFrequency(InstallmenFrequencyEnum.MONTHLY)
                .tenureUnit(TenureUnitEnum.MONTHS)
                .build();
    }


    @Test
    void testReducingBalanceScheduleCalculator_monthlyInstallment() {
        final String emi = "167.51";
        final int[] count = {0};
        StepVerifier.create(calculator.apply(monthlyInstallmentSchedulingDto))
                .expectNextCount(3)
                .assertNext(it -> {
                    count[0]++;
                    if (it.getInstallmentNumber() == 1) {
                        assertEquals(new BigDecimal(emi), it.getEmiAmount());
                        assertEquals(new BigDecimal("166.26"), it.getPrincipalComponent());
                        assertEquals(new BigDecimal("1.25"), it.getInterestComponent());
                        assertEquals(new BigDecimal("333.74"), it.getRemainingBalance());
                    }
                    if (it.getInstallmentNumber() == 3) {
                        assertEquals(new BigDecimal(emi), it.getEmiAmount());
                        assertEquals(new BigDecimal("167.10"), it.getPrincipalComponent());
                        assertEquals(new BigDecimal("0.42"), it.getInterestComponent());
                        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.CEILING), it.getRemainingBalance());
                    }
                });
//        assertEquals(3, count.length);
    }

    @Test
    void testReducingBalanceScheduleCalculator_noInstallment() {
        monthlyInstallmentSchedulingDto.setRepaymentOption(RepaymentOptionEnum.BULLET);
        final String emi = "501.25";
        final int[] count = {0};
        StepVerifier.create(calculator.apply(monthlyInstallmentSchedulingDto))
                .assertNext(it -> {
                    count[0]++;
                    assertEquals(new BigDecimal(emi), it.getEmiAmount());
                    assertEquals(new BigDecimal("500.00").setScale(2, RoundingMode.CEILING), it.getPrincipalComponent());
                    assertEquals(new BigDecimal("1.25"), it.getInterestComponent());
                    assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.CEILING), it.getRemainingBalance());
                });

        assertEquals(1, count.length);
    }

}