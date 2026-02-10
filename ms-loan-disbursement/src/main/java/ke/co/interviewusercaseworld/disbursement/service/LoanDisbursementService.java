package ke.co.interviewusercaseworld.disbursement.service;

import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.disbursement.model.dto.requests.DisbursementRequest;
import ke.co.interviewusercaseworld.disbursement.model.dto.response.DisbursementResponse;
import reactor.core.publisher.Mono;

public interface LoanDisbursementService {
    Mono<GenericResponse<DefaultResponseHeader, DisbursementResponse>> disburse(GenericRequest<DefaultResponseHeader, DisbursementRequest> request);
}
