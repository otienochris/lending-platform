package ke.co.interviewusercaseworld.repayment.services;

import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.repayment.model.dto.requests.LoanRepaymentSchedulingDto;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanRepaymentSchedulingResponse;
import org.apache.el.stream.Stream;
import reactor.core.publisher.Mono;

public interface LoanRepaymentService {
    Mono<GenericResponse<DefaultResponseHeader, LoanRepaymentSchedulingResponse>> schedule(GenericRequest<DefaultResponseHeader, LoanRepaymentSchedulingDto> disbursementRequest);
}
