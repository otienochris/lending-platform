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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                                            BigDecimal outstandingAmount = repaymentSchedules.stream().map(RepaymentSchedule::getInterestComponent).reduce(BigDecimal.ZERO, BigDecimal::add);
                                            loan.setOutstandingAmount(principal.add(outstandingAmount)); // set total outstanding balance
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
                                            return repaymentsRepository.save(repayment)
                                                    .flatMap(savedRepayment -> {
                                                        return updateScheduleTotalPaidAndStatus(isSuccessful, repaymentSchedule, amount)
                                                                .flatMap(savedRepaymentSchedule -> {
                                                                    return updateLoanStatusIfAllSchedulesAreClosed(loan);
                                                                });
                                                    }).flatMap(isUpdated -> {
                                                        String customerMessage = isSuccessful ? "Repayment successful" : "Repayment failed";
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

    private @NonNull Mono<RepaymentSchedule> updateScheduleTotalPaidAndStatus(Boolean isPaymentSuccessful, RepaymentSchedule repaymentSchedule, BigDecimal amount) {
        if (!isPaymentSuccessful) {
            return Mono.just(repaymentSchedule);
        }
        repaymentSchedule.setTotalPaid(repaymentSchedule.getTotalPaid().add(amount));
        repaymentSchedule.setStatus(repaymentSchedule.getTotalPaid().compareTo(repaymentSchedule.getEmiAmount()) >= 0 ? LoanStatusEnum.CLOSED.name() : LoanStatusEnum.OPEN.name());
        return repaymentScheduleRepository.save(repaymentSchedule);
    }

    private @NonNull Mono<Boolean> updateLoanStatusIfAllSchedulesAreClosed(Loan loan) {
        return repaymentScheduleRepository.findAllByLoanId(loan.getLoanId())
                .collectList()
                .flatMap(repaymentSchedules -> {
                    long count = repaymentSchedules.stream().filter(it -> LoanStatusEnum.OPEN.name().equalsIgnoreCase(it.getStatus())).count();
                    if (count == 0) {
                        loan.setStatus(LoanStatusEnum.CLOSED.name());
                        return loanRepository.save(loan)
                                .flatMap(savedLoan -> Mono.just(true));
                    } else {
                        return Mono.just(true);
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
