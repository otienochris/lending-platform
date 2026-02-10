package ke.co.interviewusercaseworld.repayment.services;

import ke.co.interviewusercaseworld.commons.dto.commands.RepaymentCommand;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.LoanStatusEnum;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanQueryResponseDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanRepaymentSchedulingResponse;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanValidationOrRepaymentResponseDto;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface LoanRepaymentService {
    Mono<GenericResponse<DefaultResponseHeader, LoanRepaymentSchedulingResponse>> schedule(GenericRequest<DefaultResponseHeader,
            LoanRepaymentSchedulingDto> disbursementRequest);

    Mono<GenericResponse<DefaultResponseHeader, List<LoanQueryResponseDto>>> queryLoans(UUID userId, LoanStatusEnum loanStatus);

    Mono<GenericResponse<DefaultResponseHeader, LoanValidationOrRepaymentResponseDto>> validateLoan(UUID loanScheduleId, UUID customerId);

    Mono<GenericResponse<DefaultResponseHeader, LoanValidationOrRepaymentResponseDto>> repay(RepaymentCommand repaymentCommand);
}
