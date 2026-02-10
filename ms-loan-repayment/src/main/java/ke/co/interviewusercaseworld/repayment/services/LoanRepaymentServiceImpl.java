package ke.co.interviewusercaseworld.repayment.services;

import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.enums.TenureOptionsTypeEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanRepaymentSchedulingResponse;
import ke.co.interviewusercaseworld.repayment.model.entities.Loan;
import ke.co.interviewusercaseworld.repayment.model.entities.LoanRepository;
import ke.co.interviewusercaseworld.repayment.model.entities.RepaymentSchedule;
import ke.co.interviewusercaseworld.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LoanRepaymentServiceImpl implements LoanRepaymentService {


    private final RepaymentScheduleRepository repository;
    private final LoanRepository loanRepository;
    private final TransactionalOperator operator;

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
                    .status("PENDING")
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
                .flatMap(repository::save);
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
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .outstandingAmount(request.getBody().getPrincipal()) // todo
                .build();
        return operator.transactional(
                loanRepository.save(loan)
                        .flatMapMany(savedLoan -> createSchedule(request.getBody(), firstDueDate)
                        .flatMap(repaymentSchedule -> {
                            repaymentSchedule.setLoanId(savedLoan.getId());
                            return repository.save(repaymentSchedule);
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
}
