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

import static ke.co.interviewusercaseworld.commons.utils.Helpers.calculateInstallments;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FlatRateScheduleCalculatorTest {

    LoanRepaymentSchedulingDto monthlyInstallmentSchedulingDto;
    LoanRepaymentSchedulingDto weeklyInstallmentSchedulingDto;
    @InjectMocks
    private FlatRateScheduleCalculator calculator;

    @BeforeEach
    void setUp() {
        monthlyInstallmentSchedulingDto = LoanRepaymentSchedulingDto.builder()
                .annualInterestRate(BigDecimal.valueOf(3))
                .principal(BigDecimal.valueOf(500))
                .tenure(3)
                .interestRateType(InterestRateTypeEnum.FlatRate)
                .disbursementDate(LocalDateTime.now())
                .repaymentOption(RepaymentOptionEnum.INSTALLMENT)
                .loanId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .installmentFrequency(InstallmenFrequencyEnum.MONTHLY)
                .tenureUnit(TenureUnitEnum.MONTHS)
                .build();

        weeklyInstallmentSchedulingDto = LoanRepaymentSchedulingDto.builder()
                .annualInterestRate(BigDecimal.valueOf(3))
                .principal(BigDecimal.valueOf(500))
                .tenure(3)
                .interestRateType(InterestRateTypeEnum.FlatRate)
                .disbursementDate(LocalDateTime.now())
                .repaymentOption(true)
                .loanId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .installmentFrequency(InstallmenFrequencyEnum.WEEKLY)
                .tenureUnit(TenureUnitEnum.MONTHS)
                .build();
        int weeklyInstallments = calculateInstallments(weeklyInstallmentSchedulingDto.getTenure(), weeklyInstallmentSchedulingDto.getTenureUnit(), weeklyInstallmentSchedulingDto.getInstallmentFrequency());
        weeklyInstallmentSchedulingDto.setInstallments(weeklyInstallments);
    }


    @Test
    void testFlatRateScheduleCalculator_monthlyInstallment() {
        StepVerifier.create(calculator.apply(monthlyInstallmentSchedulingDto))
                .expectNextCount(3)
                .assertNext(repaymentScheduleResponseDto -> {

                    assertEquals(BigDecimal.valueOf(167.92), repaymentScheduleResponseDto.getEmiAmount());
                    assertEquals(BigDecimal.valueOf(166.67), repaymentScheduleResponseDto.getPrincipalComponent());
                    assertEquals(BigDecimal.valueOf(1.25), repaymentScheduleResponseDto.getInterestComponent());

                    if (repaymentScheduleResponseDto.getInstallmentNumber() == 1) { // first installment
                        assertEquals(BigDecimal.valueOf(333.33), repaymentScheduleResponseDto.getRemainingBalance());
                    }

                    if (repaymentScheduleResponseDto.getInstallmentNumber() == 3) { // last
                        assertEquals(BigDecimal.ZERO, repaymentScheduleResponseDto.getRemainingBalance());
                    }
                });

    }

    @Test
    void testFlatRateScheduleCalculator_weeklyInstallment() {

        StepVerifier.create(calculator.apply(weeklyInstallmentSchedulingDto))
                .expectNextCount(12)
                .assertNext(repaymentScheduleResponseDto -> {
                    assertEquals(BigDecimal.valueOf(41.96), repaymentScheduleResponseDto.getEmiAmount());
                    assertEquals(BigDecimal.valueOf(41.67), repaymentScheduleResponseDto.getPrincipalComponent());
                    assertEquals(BigDecimal.valueOf(0.29), repaymentScheduleResponseDto.getInterestComponent());

                    if (repaymentScheduleResponseDto.getInstallmentNumber() == 1) { // first installment
                        assertEquals(BigDecimal.valueOf(458.33), repaymentScheduleResponseDto.getRemainingBalance());
                    }

                    if (repaymentScheduleResponseDto.getInstallmentNumber() == 12) { // last
                        assertEquals(BigDecimal.ZERO, repaymentScheduleResponseDto.getRemainingBalance());
                    }

                });
    }

    @Test
    void testFlatRateScheduleCalculator_notInstallment() {
        monthlyInstallmentSchedulingDto.setRepaymentOption(false); //
        StepVerifier.create(calculator.apply(monthlyInstallmentSchedulingDto))
                .expectNextCount(1)
                .assertNext(repaymentScheduleResponseDto -> {
                    assertEquals(BigDecimal.valueOf(501.25), repaymentScheduleResponseDto.getEmiAmount());
                    assertEquals(BigDecimal.valueOf(500.00).setScale(2, RoundingMode.CEILING), repaymentScheduleResponseDto.getPrincipalComponent());
                    assertEquals(BigDecimal.valueOf(1.25), repaymentScheduleResponseDto.getInterestComponent());
                    assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.CEILING), repaymentScheduleResponseDto.getRemainingBalance());
                });
    }


}