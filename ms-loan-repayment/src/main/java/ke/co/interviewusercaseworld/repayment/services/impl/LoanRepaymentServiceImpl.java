package ke.co.interviewusercaseworld.repayment.services.impl;

import ke.co.interviewusercaseworld.commons.dto.commands.RepaymentCommand;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.*;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.repayment.mappers.LoanMapper;
import ke.co.interviewusercaseworld.repayment.mappers.RepaymentScheduleMapper;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanQueryResponseDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanRepaymentSchedulingResponse;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanValidationOrRepaymentResponseDto;
import ke.co.interviewusercaseworld.repayment.model.entities.Loan;
import ke.co.interviewusercaseworld.repayment.model.entities.Repayment;
import ke.co.interviewusercaseworld.repayment.model.entities.RepaymentSchedule;
import ke.co.interviewusercaseworld.repayment.repository.LoanRepository;
import ke.co.interviewusercaseworld.repayment.repository.RepaymentScheduleRepository;
import ke.co.interviewusercaseworld.repayment.repository.RepaymentsRepository;
import ke.co.interviewusercaseworld.repayment.services.FundTransfer;
import ke.co.interviewusercaseworld.repayment.services.LoanRepaymentScheduleService;
import ke.co.interviewusercaseworld.repayment.services.LoanRepaymentService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.convertFirstCharToLowerCase;

@Service
@RequiredArgsConstructor
public class LoanRepaymentServiceImpl implements LoanRepaymentService {


    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final LoanRepository loanRepository;
    private final TransactionalOperator operator;
    private final RepaymentScheduleMapper repaymentScheduleMapper;
    private final LoanMapper loanMapper;
    private final Map<String, FundTransfer> fundTransferServices;
    private final RepaymentsRepository repaymentsRepository;
    private final LoanRepaymentScheduleService loanRepaymentScheduleService;

    private static BigDecimal getAmountToBePaid(RepaymentCommand repaymentCommand, RepaymentSchedule repaymentSchedule) {
        BigDecimal amount = repaymentCommand.getAmount();

        if (amount.compareTo(repaymentSchedule.getEmiAmount()) > 0) {
            Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "Using emi amount cause amount to be paid is large: " + amount + " : " + repaymentSchedule.getEmiAmount(), null);
            amount = repaymentSchedule.getEmiAmount(); // do not overpay
        }
        return amount;
    }

    public Flux<RepaymentSchedule> createSchedule(
            LoanRepaymentSchedulingDto dto
    ) {
        return loanRepaymentScheduleService.generateSchedule(dto)
                .map(repaymentScheduleMapper::toEntity)
                .flatMap(repaymentScheduleRepository::save);
    }

    private int resolveTenureInMonths(LoanRepaymentSchedulingDto dto) {
        if (dto.getTenureUnit() == TenureUnitEnum.YEARS) {
            return dto.getTenure() * 12;
        }
        return dto.getTenure();
    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanRepaymentSchedulingResponse>> schedule(GenericRequest<DefaultResponseHeader, LoanRepaymentSchedulingDto> request) {

        Helpers.log("", LogLevelEnum.info, OperationNameEnum.REPAYMENT_SCHEDULING, "Creating loan repayment schedule", null);
        BigDecimal principal = request.getBody().getPrincipal();
        Loan loan = Loan.builder()
                .interestRate(request.getBody().getAnnualInterestRate())
                .loanId(request.getBody().getLoanId())
                .customerId(request.getBody().getCustomerId())
                .principalAmount(principal)
                .tenure(request.getBody().getTenure())
                .status(LoanStatusEnum.OPEN.name())
                .createdAt(LocalDateTime.now())
                .outstandingAmount(principal) // todo
                .build();
        return operator.transactional(
                        loanRepository.save(loan)
                                .flatMap(savedLoan -> createSchedule(request.getBody())
                                        .flatMap(repaymentSchedule -> {
                                            repaymentSchedule.setLoanId(savedLoan.getId());
                                            repaymentSchedule.setTotalPaid(BigDecimal.ZERO);
                                            return repaymentScheduleRepository.save(repaymentSchedule); // save repayment schedule
                                        })
                                        .collectList()
                                        .flatMap(repaymentSchedules -> {
                                            BigDecimal outstandingAmount = repaymentSchedules.stream().map(RepaymentSchedule::getEmiAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                                            loan.setOutstandingAmount(outstandingAmount); // set total outstanding balance
                                            return loanRepository.save(loan);
                                        })
                                        .doOnError(throwable -> Helpers.log("", LogLevelEnum.error, OperationNameEnum.REPAYMENT_SCHEDULING, "", new RuntimeException(throwable))))
                )
                .flatMap(savedLoan -> {
                    if (savedLoan.getId() == null) {
                        return Mono.just(GenericResponse.<DefaultResponseHeader, LoanRepaymentSchedulingResponse>builder()
                                .header(DefaultResponseHeader.builder()
                                        .customerMessage("Loan creation failed")
                                        .responseCode(ResponseCodes.RC_400)
                                        .responseRefId("")
                                        .operation(request.getHeader().getOperation())
                                        .build())
                                .build());
                    }

                    return Mono.just(GenericResponse.<DefaultResponseHeader, LoanRepaymentSchedulingResponse>builder()
                            .header(DefaultResponseHeader.builder()
                                    .customerMessage("Loan repayment schedule created successfully")
                                    .operation(request.getHeader().getOperation())
                                    .responseRefId("")
                                    .responseCode(ResponseCodes.RC_200)
                                    .build())
                            .body(LoanRepaymentSchedulingResponse.builder()
                                    .totalOutstandingAmount(savedLoan.getOutstandingAmount())
                                    .interestComponent(savedLoan.getOutstandingAmount().subtract(loan.getPrincipalAmount()).setScale(2, RoundingMode.CEILING))
                                    .build())
                            .build());
                });

    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, List<LoanQueryResponseDto>>> queryLoans(UUID userId, LoanStatusEnum loanStatus) {
        return getAllByCustomerIdAndStatus(userId, loanStatus)
                .flatMap(loan -> {
                    LoanQueryResponseDto loanQueryResponseDto = loanMapper.toDto(loan);
                    return repaymentScheduleRepository.findAllByLoanId(loan.getId())
                            .map(repaymentScheduleMapper::toDto)
                            .collectList()
                            .flatMap(repaymentSchedules -> {
                                loanQueryResponseDto.setLoanRepaymentSchedule(repaymentSchedules);
                                return Mono.just(loanQueryResponseDto);
                            });
                })
                .collectList()
                .map(loanQueryResponseDtos -> {
                    if (loanQueryResponseDtos.isEmpty()) {
                        return GenericResponse.<DefaultResponseHeader, List<LoanQueryResponseDto>>builder()
                                .header(DefaultResponseHeader.builder()
                                        .customerMessage("No " + loanStatus + " loans found")
                                        .responseCode(ResponseCodes.RC_404)
                                        .responseRefId("")
                                        .operation(OperationNameEnum.LOAN_QUERY)
                                        .build())
                                .body(new ArrayList<>())
                                .build();
                    }
                    return GenericResponse.<DefaultResponseHeader, List<LoanQueryResponseDto>>builder()
                            .header(DefaultResponseHeader.builder()
                                    .customerMessage("Loan query successful")
                                    .responseCode(ResponseCodes.RC_200)
                                    .responseRefId("")
                                    .operation(OperationNameEnum.LOAN_QUERY)
                                    .build())
                            .body(loanQueryResponseDtos)
                            .build();
                });
    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanValidationOrRepaymentResponseDto>> validateLoan(UUID loanScheduleId, UUID customerId) {

        Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "validating loan schedule: " + loanScheduleId, null);
        return repaymentScheduleRepository.findById(loanScheduleId).defaultIfEmpty(RepaymentSchedule.builder().build())
                .flatMap(repaymentSchedule -> {
                    return loanRepository.findById(repaymentSchedule.getLoanId()).defaultIfEmpty(Loan.builder().build())
                            .defaultIfEmpty(Loan.builder().build())
                            .flatMap(loan -> {
                                if (!loan.getCustomerId().equals(customerId)) {
                                    return Mono.just(GenericResponse.<DefaultResponseHeader, LoanValidationOrRepaymentResponseDto>builder()
                                            .header(DefaultResponseHeader.builder()
                                                    .customerMessage("Loan not found for customer")
                                                    .responseCode(ResponseCodes.RC_404)
                                                    .responseRefId("")
                                                    .debugMessage("Loan not found for customer")
                                                    .build())
                                            .build());
                                }
                                return Mono.just(GenericResponse.<DefaultResponseHeader, LoanValidationOrRepaymentResponseDto>builder()
                                        .header(DefaultResponseHeader.builder()
                                                .customerMessage("Loan validation successful")
                                                .responseCode(ResponseCodes.RC_200)
                                                .responseRefId("")
                                                .debugMessage("Loan validation successful")
                                                .build())
                                        .body(LoanValidationOrRepaymentResponseDto.builder()
                                                .isSuccessful(true)
                                                .build())
                                        .build());
                            });
                });


    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanValidationOrRepaymentResponseDto>> repay(RepaymentCommand repaymentCommand) {

        return repaymentScheduleRepository.findById(repaymentCommand.getLoanScheduleId())
                .flatMap(repaymentSchedule -> {
                    return loanRepository.findById(repaymentSchedule.getLoanId())
                            .flatMap(loan -> {
                                String mode = repaymentCommand.getWalletType().name(); // payment mode
                                FundTransfer chosenFundTransfer = getChosenFundTransfer(mode);

                                BigDecimal amount = getAmountToBePaid(repaymentCommand, repaymentSchedule);
                                return chosenFundTransfer.receive(amount, repaymentCommand.getWalletId())
                                        .flatMap(isSuccessful -> {

                                            Repayment repayment = Repayment.builder()
                                                    .amountPaid(amount)
                                                    .paymentDate(LocalDateTime.now())
                                                    .paymentMode(mode)
                                                    .loanId(repaymentSchedule.getLoanId())
                                                    .scheduleId(repaymentSchedule.getScheduleId())
                                                    .status(isSuccessful ? "SUCCESSFUL" : "FAILED")
                                                    .build();

                                            BigDecimal totalDue = loan.getOutstandingAmount().subtract(amount);
                                            loan.setOutstandingAmount(totalDue);
                                            loan.setStatus(totalDue.compareTo(BigDecimal.ZERO) <= 0 ? LoanStatusEnum.CLOSED.name() : loan.getStatus());

                                            BigDecimal emiAmount = repaymentSchedule.getEmiAmount();
                                            BigDecimal outstandingEmi = emiAmount.subtract(amount);
                                            System.out.println("EMI change: " + emiAmount.toPlainString() + "->" + outstandingEmi.toPlainString());
                                            repaymentSchedule.setEmiAmount(outstandingEmi);
                                            repaymentSchedule.setStatus(repaymentSchedule.getEmiAmount().compareTo(BigDecimal.ZERO) <= 0 ? LoanStatusEnum.CLOSED.name() : repaymentSchedule.getStatus());
                                            repaymentSchedule.setTotalPaid(repaymentSchedule.getTotalPaid().add(amount));

                                            Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "Saving repayment", null);
                                            System.out.println(loan);
                                            System.out.println(repaymentSchedule);
                                            return repaymentsRepository.save(repayment)
                                                    .flatMap(savedRepayment -> {
                                                        return saveLoanAndScheduleUpdates(isSuccessful, repaymentSchedule, loan)
                                                                .flatMap(savedRepaymentSchedule -> {
                                                                    return updateLoanStatusIfAllSchedulesAreClosedV2(loan, savedRepaymentSchedule);
                                                                });
                                                    }).flatMap(isUpdated -> {
                                                        String customerMessage = isSuccessful ? "Repayment successful" : "Repayment failed";
                                                        Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, customerMessage, null);
                                                        ResponseCodes responseCode = isSuccessful ? ResponseCodes.RC_200 : ResponseCodes.RC_400;
                                                        GenericResponse<DefaultResponseHeader, LoanValidationOrRepaymentResponseDto> response = GenericResponse.<DefaultResponseHeader, LoanValidationOrRepaymentResponseDto>builder()
                                                                .header(DefaultResponseHeader.builder()
                                                                        .customerMessage(customerMessage)
                                                                        .responseCode(responseCode)
                                                                        .responseRefId("")
                                                                        .debugMessage(customerMessage)
                                                                        .build())
                                                                .body(LoanValidationOrRepaymentResponseDto.builder()
                                                                        .isSuccessful(isSuccessful)
                                                                        .build())
                                                                .build();
                                                        return Mono.just(response);
                                                    });


                                        });

                            });
                });
    }

    private @NonNull Mono<RepaymentSchedule> saveLoanAndScheduleUpdates(Boolean isPaymentSuccessful,
                                                                        RepaymentSchedule repaymentSchedule, Loan loan) {

        if (!isPaymentSuccessful) {
            Helpers.log("", LogLevelEnum.warn, OperationNameEnum.LOAN_REPAYMENT, "Payment failed. loan and schedule are not being update", null);
            return Mono.just(repaymentSchedule);
        }
        return loanRepository.save(loan)
                .flatMap(updatedLoan -> repaymentScheduleRepository.save(repaymentSchedule));

    }


    private @NonNull Mono<Boolean> updateLoanStatusIfAllSchedulesAreClosedV2(Loan loan, RepaymentSchedule savedRepaymentSchedule) {

        if (LoanStatusEnum.CLOSED.name().equalsIgnoreCase(loan.getStatus())) { // if loan status is closed, close all schedules
            Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "Updating loan schedule status to CLOSED cause the loan status is closed", null);
            return repaymentScheduleRepository.findAllByLoanId(loan.getLoanId())
                    .flatMap(repaymentSchedule -> {
                        repaymentSchedule.setStatus(LoanStatusEnum.CLOSED.name());
                        return Mono.just(repaymentScheduleRepository.save(repaymentSchedule));
                    }).then(Mono.defer(() -> Mono.just(true)));
        } else if (!LoanStatusEnum.CLOSED.name().equalsIgnoreCase(savedRepaymentSchedule.getStatus())) {
            Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "Installment still ongoing: " + savedRepaymentSchedule.getScheduleId(), null);
            return repaymentScheduleRepository.save(savedRepaymentSchedule)
                    .thenReturn(true);
        } else {
            return repaymentScheduleRepository.findAllByLoanIdAndStatusNotIn(loan.getId(), List.of(LoanStatusEnum.CLOSED.name()))
                    .collectList()
                    .defaultIfEmpty(List.of())
                    .flatMap(repaymentSchedules -> {
                        if (repaymentSchedules.isEmpty()) {
                            Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "All schedules are closed", null);
                            return Mono.defer(() -> Mono.just(true));
                        }


                        List<RepaymentSchedule> notClosedRepaymentSchedules = repaymentSchedules.stream()
                                .filter(it -> {
                                    boolean isPendingSchedule = LoanStatusEnum.PENDING.name().equalsIgnoreCase(it.getStatus());
                                    // System.out.println("Comparing: ! (PENDING=" + it.getStatus() + ")? " + isPendingSchedule);
                                    return isPendingSchedule;
                                })
                                .sorted(Comparator.comparing(RepaymentSchedule::getInstallmentNumber))
                                .toList();

                        Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "Some " + notClosedRepaymentSchedules.size() + " schedules are PENDING. Getting next schedule", null);

                        AtomicReference<RepaymentSchedule> nextSchedule = new AtomicReference<>();
                        notClosedRepaymentSchedules.stream().findFirst().ifPresentOrElse(nextSchedule::set, () -> Helpers.log("", LogLevelEnum.warn, OperationNameEnum.LOAN_REPAYMENT, "did not find pending schedule", null));

                        RepaymentSchedule repaymentSchedule = nextSchedule.get();
                        if (repaymentSchedule != null) {
                            Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "Found next schedule: " + repaymentSchedule.getScheduleId(), null);
                            repaymentSchedule.setStatus(LoanStatusEnum.OPEN.name());
                            return repaymentScheduleRepository.save(repaymentSchedule)
                                    .thenReturn(true);
                        } else {
                            return Mono.defer(() -> Mono.just(false));
                        }
                    });
        }
    }

    private @NonNull Mono<Boolean> updateLoanStatusIfAllSchedulesAreClosed(Loan loan, RepaymentSchedule savedRepaymentSchedule) {
        if (!LoanStatusEnum.CLOSED.name().equalsIgnoreCase(savedRepaymentSchedule.getStatus())) {
            Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "Installment still ongoing: " + savedRepaymentSchedule.getScheduleId(), null);
            return Mono.just(true);
        }
        return repaymentScheduleRepository.findAllByLoanId(loan.getLoanId())
                .collectList()
                .flatMap(repaymentSchedules -> {

                    List<RepaymentSchedule> notClosedRepaymentSchedules = repaymentSchedules.stream().filter(it -> !LoanStatusEnum.CLOSED.name().equalsIgnoreCase(it.getStatus()))
                            .sorted(Comparator.comparing(RepaymentSchedule::getInstallmentNumber))
                            .toList();

                    long count = notClosedRepaymentSchedules.size();
                    if (count == 0) { //if there are no non-closed schedules eg. PENDING,
                        Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "All schedules are closed. Update loan status to closed", null);
                        loan.setStatus(LoanStatusEnum.CLOSED.name()); // close the loan
                        return loanRepository.save(loan)
                                .flatMap(savedLoan -> Mono.just(true));
                    } else {

                        Helpers.log("", LogLevelEnum.info, OperationNameEnum.LOAN_REPAYMENT, "Some " + notClosedRepaymentSchedules.size() + " schedules are not closed. Getting next schedule", null);

                        AtomicReference<RepaymentSchedule> nextSchedule = new AtomicReference<>();
                        notClosedRepaymentSchedules.stream().findFirst().ifPresent(nextSchedule::set);

                        RepaymentSchedule repaymentSchedule = nextSchedule.get();
                        repaymentSchedule.setStatus(LoanStatusEnum.OPEN.name());
                        return repaymentScheduleRepository.save(repaymentSchedule)
                                .thenReturn(true);
                    }
                });
    }

    private Flux<Loan> getAllByCustomerIdAndStatus(UUID userId, LoanStatusEnum loanStatus) {
        if (loanStatus == null) {
            return loanRepository.findAllByCustomerId(userId);
        }
        return loanRepository.findAllByCustomerIdAndStatus(userId, loanStatus);
    }

    private FundTransfer getChosenFundTransfer(String name) {
        System.out.println("Available Fund transfer services:");
        fundTransferServices.keySet().forEach(System.out::println);
        String key = convertFirstCharToLowerCase(name + "FundTransferService");
        return fundTransferServices.get(key);
    }
}
