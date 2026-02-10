package ke.co.interviewusercaseworld.msorchestrator.service;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanRepaymentRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationAcknowledgement;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanRepaymentRequestAck;
import org.apache.el.stream.Stream;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface LoanService {
    Mono<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>> apply(@RequestBody GenericRequest<DefaultRequestHeader, LoanApplicationRequest> loanApplicationRequest);

    Mono<GenericResponse<DefaultResponseHeader, LoanRepaymentRequestAck>> repay(GenericRequest<DefaultRequestHeader, LoanRepaymentRequest> request);
}
