package ke.co.interviewusercaseworld.productconfig.service;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.LoanProductCreationRequest;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.LoanProductCreationResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public interface LoanProductService {

    Mono<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>> createLoanProduct(GenericRequest<DefaultRequestHeader, LoanProductCreationRequest> loanProductCreationRequest);
    Mono<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>> updateLoanProduct(UUID loanProductId, GenericRequest<DefaultRequestHeader, LoanProductCreationRequest> loanProductCreationRequest);
    Mono<GenericResponse<DefaultResponseHeader, Void>> deleteLoanProduct(Map<String, String> headers, UUID loanProductId);
    Mono<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>> getLoanProduct(UUID loanProductId, Map<String, String> headers);
    Flux<LoanProductCreationResponseDto> getAllLoanProducts(Map<String, String> headers);
}
