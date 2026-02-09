package ke.co.interviewusercaseworld.msorchestrator.service;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface LoanProductService {
    Mono<GenericResponse<DefaultResponseHeader, LoanApplicationResponse>> validateProduct(Map<String, String> headers,
                                                                                          GenericRequest<DefaultRequestHeader, LoanApplicationRequest> request);
}
