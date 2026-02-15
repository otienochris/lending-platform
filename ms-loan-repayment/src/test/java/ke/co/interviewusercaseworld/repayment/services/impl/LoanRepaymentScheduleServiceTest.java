package ke.co.interviewusercaseworld.repayment.services.impl;

import ke.co.interviewusercaseworld.commons.enums.InstallmenFrequencyEnum;
import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import ke.co.interviewusercaseworld.commons.enums.RepaymentOptionEnum;
import ke.co.interviewusercaseworld.commons.enums.TenureUnitEnum;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.RepaymentScheduleResponseDto;
import ke.co.interviewusercaseworld.repayment.services.ScheduleCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanRepaymentScheduleServiceTest {

    LoanRepaymentSchedulingDto flatRateRequest;
    LoanRepaymentSchedulingDto reducingBalanceRequest;
    List<RepaymentScheduleResponseDto> flatRateResponse;
    @Mock
    private Map<String, ScheduleCalculator> scheduleCalculators;
    @Mock
    private ScheduleCalculator scheduleCalculator;
    @InjectMocks
    private LoanRepaymentScheduleServiceImpl loanRepaymentScheduleServiceImpl;

    @BeforeEach
    void setUp() {

        // requests
        UUID uuid = UUID.randomUUID();
        LocalDateTime disbursementDate = LocalDateTime.now();
        LoanRepaymentSchedulingDto genericRequest = LoanRepaymentSchedulingDto.builder()
                .tenure(3)
                .tenureUnit(TenureUnitEnum.MONTHS)
                .loanId(uuid)
                .interestRateType(InterestRateTypeEnum.FlatRate)
                .repaymentOption(RepaymentOptionEnum.INSTALLMENT)
                .installmentFrequency(InstallmenFrequencyEnum.MONTHLY)
                .productId(uuid)
                .customerId(uuid)
                .principal(BigDecimal.valueOf(5000))
                .annualInterestRate(BigDecimal.valueOf(11))
                .disbursementDate(disbursementDate)
                .build();
        flatRateRequest = genericRequest;

        genericRequest.setInterestRateType(InterestRateTypeEnum.ReducingBalance);
        genericRequest.setTenure(1);
        genericRequest.setDisbursementDate(disbursementDate.plusMonths(1));
        genericRequest.setTenureUnit(TenureUnitEnum.YEARS);
        reducingBalanceRequest = genericRequest;

        // response
        List<RepaymentScheduleResponseDto> repaymentSchedules = new ArrayList<>();
        repaymentSchedules.add(RepaymentScheduleResponseDto.builder()
                .installmentNumber(1)
                .scheduleId(uuid)
                .dueDate(disbursementDate.plusMonths(1))
                .status("PENDING")
                .build());
        repaymentSchedules.add(RepaymentScheduleResponseDto.builder()
                .installmentNumber(2)
                .scheduleId(uuid)
                .dueDate(disbursementDate.plusMonths(2))
                .status("PENDING")
                .build());
        repaymentSchedules.add(RepaymentScheduleResponseDto.builder()
                .installmentNumber(3)
                .scheduleId(uuid)
                .dueDate(disbursementDate.plusMonths(3))
                .status("PENDING")
                .build());
        flatRateResponse = repaymentSchedules;
    }


    @Test
    void testFlatRateScheduleCalculator_success() {

        when(scheduleCalculators.get(anyString())).thenReturn(scheduleCalculator);
        when(scheduleCalculator.apply(any())).thenAnswer((it) -> Flux.fromIterable(flatRateResponse));

        StepVerifier
                .create(loanRepaymentScheduleServiceImpl.generateSchedule(flatRateRequest).collectList())
                .assertNext(flatRateResponse -> {
                    assertThat(flatRateResponse).hasSize(3);
                })
                .verifyComplete();

    }

}