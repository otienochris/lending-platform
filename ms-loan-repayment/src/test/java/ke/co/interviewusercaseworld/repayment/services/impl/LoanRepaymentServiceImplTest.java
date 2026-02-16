package ke.co.interviewusercaseworld.repayment.services.impl;

import ke.co.interviewusercaseworld.commons.dto.commands.RepaymentCommand;
import ke.co.interviewusercaseworld.commons.enums.*;
import ke.co.interviewusercaseworld.repayment.mappers.LoanMapper;
import ke.co.interviewusercaseworld.repayment.mappers.RepaymentScheduleMapper;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.RepaymentScheduleResponseDto;
import ke.co.interviewusercaseworld.repayment.model.entities.Loan;
import ke.co.interviewusercaseworld.repayment.model.entities.Repayment;
import ke.co.interviewusercaseworld.repayment.model.entities.RepaymentSchedule;
import ke.co.interviewusercaseworld.repayment.repository.LoanRepository;
import ke.co.interviewusercaseworld.repayment.repository.RepaymentScheduleRepository;
import ke.co.interviewusercaseworld.repayment.repository.RepaymentsRepository;
import ke.co.interviewusercaseworld.repayment.services.FundTransfer;
import ke.co.interviewusercaseworld.repayment.services.LoanRepaymentScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanRepaymentServiceImplTest {

    LoanRepaymentSchedulingDto schedulingDto;
    UUID loanId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private TransactionalOperator operator;
    @Mock
    private RepaymentScheduleMapper repaymentScheduleMapper;
    @Mock
    private LoanMapper loanMapper;
    @Mock
    private Map<String, FundTransfer> fundTransferServices;
    @Mock
    private MobileMoneyFundTransferService mobileMoneyFundTransferService;
    @Mock
    private RepaymentsRepository repaymentsRepository;
    @Mock
    private LoanRepaymentScheduleService loanRepaymentScheduleService;
    @InjectMocks
    private LoanRepaymentServiceImpl loanRepaymentService;

    @BeforeEach
    void setup() {

        schedulingDto = LoanRepaymentSchedulingDto.builder()
                .loanId(loanId)
                .customerId(customerId)
                .principal(BigDecimal.valueOf(500))
                .tenureUnit(TenureUnitEnum.MONTHS)
                .disbursementDate(LocalDateTime.now())
                .annualInterestRate(BigDecimal.valueOf(10))
                .installmentFrequency(InstallmenFrequencyEnum.MONTHLY)
                .tenure(3)
                .repaymentOption(RepaymentOptionEnum.INSTALLMENT)
                .interestRateType(InterestRateTypeEnum.ReducingBalance)
                .productId(productId)
                .build();
    }

    @Test
    void createSchedule() {

        RepaymentScheduleResponseDto scheduleResponseDto = RepaymentScheduleResponseDto.builder()
                .scheduleId(UUID.randomUUID())
                .loanId(loanId)
                .remainingBalance(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .build();
        RepaymentSchedule repaymentSchedule = RepaymentSchedule.builder()
                .loanId(loanId)
                .totalPaid(BigDecimal.ZERO)
                .emiAmount(BigDecimal.valueOf(500))
                .build();

        when(loanRepaymentScheduleService.generateSchedule(schedulingDto)).thenReturn(Flux.fromIterable(List.of(scheduleResponseDto, scheduleResponseDto, scheduleResponseDto)));

        StepVerifier.create(loanRepaymentService.createSchedule(schedulingDto))
                .assertNext(schedule -> {
                    assertThat(schedule.getLoanId()).isEqualTo(loanId);
                    assertThat(schedule.getTotalPaid()).isEqualTo(BigDecimal.ZERO);
                    assertThat(schedule.getEmiAmount()).isEqualTo(BigDecimal.valueOf(500));

                });
//                .verifyComplete();
    }


    @Test
    void repay_partial_payment() {
        UUID loanScheduleId = UUID.randomUUID();
        RepaymentCommand repaymentCommand = RepaymentCommand.builder()
                .loanScheduleId(loanScheduleId)
                .walletType(WalletTypeEnum.MobileMoney)
                .walletId("254742887480")
                .amount(BigDecimal.valueOf(100))
                .build();

        RepaymentSchedule repaymentSchedule = RepaymentSchedule.builder()
                .scheduleId(loanScheduleId)
                .installmentNumber(1)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.OPEN.name())
                .build();

        RepaymentSchedule newRepaymentSchedule = RepaymentSchedule.builder()
                .scheduleId(loanScheduleId)
                .installmentNumber(1)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(400))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.OPEN.name())
                .build();

        RepaymentSchedule repaymentSchedule2 = RepaymentSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .installmentNumber(2)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.PENDING.name())
                .build();

        RepaymentSchedule repaymentSchedule3 = RepaymentSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .installmentNumber(3)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.PENDING.name())
                .build();
        Loan loan = Loan.builder().loanId(loanId).outstandingAmount(BigDecimal.valueOf(1500)).build();

        when(repaymentScheduleRepository.findById(any(UUID.class))).thenReturn(Mono.just(repaymentSchedule));
        when(loanRepository.findById(any(UUID.class))).thenReturn(Mono.just(loan));
        when(fundTransferServices.get("mobileMoneyFundTransferService")).thenReturn(mobileMoneyFundTransferService);
        when(mobileMoneyFundTransferService.receive(repaymentCommand.getAmount(), repaymentCommand.getWalletId())).thenReturn(Mono.just(Boolean.TRUE));
        when(repaymentsRepository.save(any())).thenReturn(Mono.just(Repayment.builder().build()));
        when(loanRepository.save(any())).thenReturn(Mono.just(loan));
        when(repaymentScheduleRepository.save(any())).thenReturn(Mono.just(newRepaymentSchedule));


        StepVerifier.create(loanRepaymentService.repay(repaymentCommand))
                .assertNext(repayment -> {
                    assertThat(ResponseCodes.RC_200.equals(repayment.getHeader().getResponseCode())).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void repay_full_schedule_payment() {
        UUID loanScheduleId = UUID.randomUUID();
        RepaymentCommand repaymentCommand = RepaymentCommand.builder()
                .loanScheduleId(loanScheduleId)
                .walletType(WalletTypeEnum.MobileMoney)
                .walletId("254742887480")
                .amount(BigDecimal.valueOf(500))
                .build();

        RepaymentSchedule repaymentSchedule = RepaymentSchedule.builder()
                .scheduleId(loanScheduleId)
                .installmentNumber(1)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.OPEN.name())
                .build();

        RepaymentSchedule newRepaymentSchedule = RepaymentSchedule.builder()
                .scheduleId(loanScheduleId)
                .installmentNumber(1)
                .loanId(loanId)
                .emiAmount(BigDecimal.ZERO)
                .totalPaid(repaymentCommand.getAmount())
                .status(LoanStatusEnum.CLOSED.name())
                .build();

        RepaymentSchedule repaymentSchedule2 = RepaymentSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .installmentNumber(2)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.PENDING.name())
                .build();

        RepaymentSchedule repaymentSchedule3 = RepaymentSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .installmentNumber(3)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.PENDING.name())
                .build();
        Loan loan = Loan.builder().loanId(loanId).outstandingAmount(BigDecimal.valueOf(1500)).status(LoanStatusEnum.OPEN.name()).build();

        when(repaymentScheduleRepository.findById(any(UUID.class))).thenReturn(Mono.just(repaymentSchedule));
        when(fundTransferServices.get("mobileMoneyFundTransferService")).thenReturn(mobileMoneyFundTransferService);
        when(mobileMoneyFundTransferService.receive(repaymentCommand.getAmount(), repaymentCommand.getWalletId())).thenReturn(Mono.just(Boolean.TRUE));
        when(repaymentsRepository.save(any())).thenReturn(Mono.just(Repayment.builder().build()));
        when(loanRepository.findById(any(UUID.class))).thenReturn(Mono.just(loan));
        when(loanRepository.save(any())).thenReturn(Mono.just(loan));
        when(repaymentScheduleRepository.save(any())).thenReturn(Mono.just(newRepaymentSchedule));
        when(repaymentScheduleRepository.findAllByLoanId(any(UUID.class))).thenReturn(Flux.just(repaymentSchedule, repaymentSchedule2, repaymentSchedule3));


        StepVerifier.create(loanRepaymentService.repay(repaymentCommand))
                .assertNext(repayment -> {
                    assertThat(ResponseCodes.RC_200.equals(repayment.getHeader().getResponseCode())).isTrue();
                })
                .verifyComplete();
    }


    @Test
    void repay_full_loan_payment() {
        UUID loanScheduleId = UUID.randomUUID();
        RepaymentCommand repaymentCommand = RepaymentCommand.builder()
                .loanScheduleId(loanScheduleId)
                .walletType(WalletTypeEnum.MobileMoney)
                .walletId("254742887480")
                .amount(BigDecimal.valueOf(500))
                .build();

        RepaymentSchedule repaymentSchedule = RepaymentSchedule.builder()
                .scheduleId(loanScheduleId)
                .installmentNumber(1)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.CLOSED.name())
                .build();

        RepaymentSchedule newRepaymentSchedule = RepaymentSchedule.builder()
                .scheduleId(loanScheduleId)
                .installmentNumber(1)
                .loanId(loanId)
                .emiAmount(BigDecimal.ZERO)
                .totalPaid(repaymentCommand.getAmount())
                .status(LoanStatusEnum.CLOSED.name())
                .build();

        RepaymentSchedule repaymentSchedule2 = RepaymentSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .installmentNumber(2)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.CLOSED.name())
                .build();

        RepaymentSchedule repaymentSchedule3 = RepaymentSchedule.builder()
                .scheduleId(UUID.randomUUID())
                .installmentNumber(3)
                .loanId(loanId)
                .emiAmount(BigDecimal.valueOf(500))
                .totalPaid(BigDecimal.ZERO)
                .status(LoanStatusEnum.OPEN.name())
                .build();
        Loan loan = Loan.builder().loanId(loanId).outstandingAmount(BigDecimal.valueOf(500)).status(LoanStatusEnum.OPEN.name()).build();
        Loan saveLoan = Loan.builder().loanId(loanId).outstandingAmount(BigDecimal.ZERO).status(LoanStatusEnum.CLOSED.name()).build();

        when(repaymentScheduleRepository.findById(any(UUID.class))).thenReturn(Mono.just(repaymentSchedule3));
        when(fundTransferServices.get("mobileMoneyFundTransferService")).thenReturn(mobileMoneyFundTransferService);
        when(mobileMoneyFundTransferService.receive(repaymentCommand.getAmount(), repaymentCommand.getWalletId())).thenReturn(Mono.just(Boolean.TRUE));
        when(repaymentsRepository.save(any())).thenReturn(Mono.just(Repayment.builder().build()));
        when(loanRepository.findById(any(UUID.class))).thenReturn(Mono.just(loan));
        when(loanRepository.save(any())).thenReturn(Mono.just(saveLoan));
        when(repaymentScheduleRepository.save(any())).thenReturn(Mono.just(newRepaymentSchedule));
        when(repaymentScheduleRepository.findAllByLoanId(any(UUID.class))).thenReturn(Flux.just(repaymentSchedule, repaymentSchedule2, repaymentSchedule3));


        StepVerifier.create(loanRepaymentService.repay(repaymentCommand))
                .assertNext(repayment -> {
                    assertThat(ResponseCodes.RC_200.equals(repayment.getHeader().getResponseCode())).isTrue();
                })
                .verifyComplete();
    }
}