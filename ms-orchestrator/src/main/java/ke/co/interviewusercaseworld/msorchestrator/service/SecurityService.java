package ke.co.interviewusercaseworld.msorchestrator.service;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.dto.responses.UserValidationResponse;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface SecurityService {
    Mono<GenericResponse<DefaultResponseHeader, UserValidationResponse>> validateUser(Map<String, String> headers, GenericRequest<DefaultRequestHeader, LoanApplicationRequest> request);
}
