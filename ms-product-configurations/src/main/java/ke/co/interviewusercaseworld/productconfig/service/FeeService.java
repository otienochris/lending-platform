package ke.co.interviewusercaseworld.productconfig.service;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.FeeRequestDto;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.FeeResponseDto;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FeeService {
    Mono<GenericResponse<DefaultResponseHeader, List<FeeResponseDto>>> addFees(UUID productId, GenericRequest<DefaultRequestHeader, List<FeeRequestDto>> request);

    Mono<GenericResponse<DefaultResponseHeader, List<FeeResponseDto>>> getFees(UUID productId, Map<String, String> headers);

    Mono<GenericResponse<DefaultResponseHeader, Void>> deleteFee(Map<String, String> headers, UUID feeId);

}
