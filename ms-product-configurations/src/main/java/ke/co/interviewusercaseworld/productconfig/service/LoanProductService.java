package ke.co.interviewusercaseworld.productconfig.service;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.dto.responses.LoanProductResponseDto;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.LoanProductCreationRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public interface LoanProductService {

    Mono<GenericResponse<DefaultResponseHeader, LoanProductResponseDto>> createLoanProduct(GenericRequest<DefaultRequestHeader, LoanProductCreationRequest> loanProductCreationRequest);

    Mono<GenericResponse<DefaultResponseHeader, LoanProductResponseDto>> updateLoanProduct(UUID loanProductId, GenericRequest<DefaultRequestHeader, LoanProductCreationRequest> loanProductCreationRequest);
    Mono<GenericResponse<DefaultResponseHeader, Void>> deleteLoanProduct(Map<String, String> headers, UUID loanProductId);

    Mono<GenericResponse<DefaultResponseHeader, LoanProductResponseDto>> getLoanProduct(UUID loanProductId, Map<String, String> headers);

    Flux<LoanProductResponseDto> getAllLoanProducts(Map<String, String> headers);
}
