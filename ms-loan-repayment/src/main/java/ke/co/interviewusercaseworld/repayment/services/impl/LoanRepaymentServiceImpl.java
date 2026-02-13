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
import ke.co.interviewusercaseworld.repayment.services.LoanRepaymentService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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

    public Flux<RepaymentSchedule> createSchedule(
            LoanRepaymentSchedulingDto dto,
            LocalDate firstDueDate
    ) {

        if (!dto.getIsInstallment()) {
            BigDecimal interest = switch (dto.getTenureType()) {
                case DAYS -> dto.getPrincipal().multiply(dto.getInterestRate()).divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
                case MONTHS -> dto.getPrincipal().multiply(dto.getInterestRate()).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                case YEARS -> dto.getPrincipal().multiply(dto.getInterestRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                default -> BigDecimal.ONE;
            };

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dueDate = switch (dto.getTenureType()) {
                case DAYS -> now.plusDays(dto.getTenure());
                case MONTHS -> now.plusMonths(dto.getTenure());
                case YEARS -> now.plusYears(dto.getTenure());
                default -> now;
            };

            BigDecimal total = dto.getPrincipal().add(interest);
            return Flux.just(RepaymentSchedule.builder()
                    .loanId(dto.getLoanId())
                    .dueDate(dueDate.toLocalDate())
                    .emiAmount(total)
                    .principalComponent(dto.getPrincipal())
                    .interestComponent(interest)
                    .status(LoanStatusEnum.OPEN.name())
                    .build());
        }

        int months = resolveTenureInMonths(dto);
        final BigDecimal[] emi = {Helpers.calculateEmi(
                dto.getPrincipal(),
                dto.getInterestRate(),
                months
        )};

        BigDecimal monthlyRate = dto.getInterestRate()
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        AtomicReference<BigDecimal> outstanding =
                new AtomicReference<>(dto.getPrincipal());

        return Flux.range(1, months)
                .map(i -> {
                    BigDecimal interest =
                            outstanding.get().multiply(monthlyRate)
                                    .setScale(2, RoundingMode.HALF_UP);

                    BigDecimal principalComponent =
                            emi[0].subtract(interest)
                                    .setScale(2, RoundingMode.HALF_UP);

                    // Last installment adjustment
                    if (i == months) {
                        principalComponent = outstanding.get();
                        emi[0] = principalComponent.add(interest);
                    }

                    outstanding.set(outstanding.get().subtract(principalComponent));

                    return RepaymentSchedule.builder()
                            .loanId(dto.getLoanId())
                            .dueDate(firstDueDate.plusMonths(i - 1))
                            .emiAmount(emi[0])
                            .principalComponent(principalComponent)
                            .interestComponent(interest)
                            .status("PENDING")
                            .build();
                })
                .flatMap(repaymentScheduleRepository::save);
    }

    private int resolveTenureInMonths(LoanRepaymentSchedulingDto dto) {
        if (dto.getTenureType() == TenureOptionsTypeEnum.YEARS) {
            return dto.getTenure() * 12;
        }
        return dto.getTenure();
    }


    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanRepaymentSchedulingResponse>> schedule(GenericRequest<DefaultResponseHeader, LoanRepaymentSchedulingDto> request) {

        LocalDate firstDueDate = switch (request.getBody().getTenureType()) {
            case DAYS -> LocalDate.now().plusDays(1);
            case MONTHS, YEARS -> LocalDate.now().plusMonths(1);
        };
        Helpers.log("", LogLevelEnum.info, OperationNameEnum.REPAYMENT_SCHEDULING, "Creating loan repayment schedule", null);
        Loan loan = Loan.builder()
                .interestRate(request.getBody().getInterestRate())
                .loanId(request.getBody().getLoanId())
                .customerId(request.getBody().getCustomerId())
                .principalAmount(request.getBody().getPrincipal())
                .tenureMonths(resolveTenureInMonths(request.getBody()))
                .status(LoanStatusEnum.OPEN.name())
                .createdAt(LocalDateTime.now())
                .outstandingAmount(request.getBody().getPrincipal()) // todo
                .build();
        return operator.transactional(
                loanRepository.save(loan)
                        .flatMapMany(savedLoan -> createSchedule(request.getBody(), firstDueDate)
                        .flatMap(repaymentSchedule -> {
                            repaymentSchedule.setLoanId(savedLoan.getId());
                            repaymentSchedule.setTotalPaid(BigDecimal.ZERO);
                            return repaymentScheduleRepository.save(repaymentSchedule);
                        })
                        .doOnError(throwable -> Helpers.log("", LogLevelEnum.error, OperationNameEnum.REPAYMENT_SCHEDULING, "", new RuntimeException(throwable))))
                ).collectList()
                .flatMap(repaymentSchedule -> {
                    BigDecimal totalOutstanding = repaymentSchedule.stream().map(RepaymentSchedule::getEmiAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalInterest = repaymentSchedule.stream().map(RepaymentSchedule::getInterestComponent).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return Mono.just(GenericResponse.<DefaultResponseHeader, LoanRepaymentSchedulingResponse>builder()
                            .header(DefaultResponseHeader.builder()
                                    .customerMessage("Loan repayment schedule created successfully")
                                    .operation(request.getHeader().getOperation())
                                    .responseRefId("")
                                    .responseCode(ResponseCodes.RC_200)
                                    .build())
                                    .body(LoanRepaymentSchedulingResponse.builder()
                                            .totalOutstandingAmount(totalOutstanding)
                                            .totalInterest(totalInterest)
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
                                        .customerMessage("No " + loanStatus +" loans found")
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
                                return chosenFundTransfer.receive(amount,repaymentCommand.getWalletId())
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
        repaymentSchedule.setStatus(repaymentSchedule.getTotalPaid().compareTo(repaymentSchedule.getEmiAmount()) >=0 ? LoanStatusEnum.CLOSED.name() : LoanStatusEnum.OPEN.name());
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

    private static BigDecimal getAmountToBePaid(RepaymentCommand repaymentCommand, RepaymentSchedule repaymentSchedule) {
        BigDecimal amount = repaymentCommand.getAmount();
        if (amount.compareTo(repaymentSchedule.getEmiAmount()) > 0) {
            amount = repaymentSchedule.getEmiAmount(); // do not overpay
        }
        return amount;
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

        String key = name + "FundTransferService";
        StringBuilder sb = new StringBuilder(key);
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        System.out.println("Chosen fund transfer service: " + sb);
        return fundTransferServices.get(sb.toString());
    }
}
