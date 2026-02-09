package ke.co.interviewusercaseworld.productconfig.service.impl;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.productconfig.mappers.ProductFeeMapper;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.FeeRequestDto;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.FeeResponseDto;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.LoanProductCreationResponseDto;
import ke.co.interviewusercaseworld.productconfig.model.entity.ProductFee;
import ke.co.interviewusercaseworld.productconfig.repository.ProductFeeRepository;
import ke.co.interviewusercaseworld.productconfig.service.FeeService;
import ke.co.interviewusercaseworld.productconfig.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ke.co.interviewusercaseworld.commons.enums.ResponseCodes.RC_200;
import static ke.co.interviewusercaseworld.commons.enums.ResponseCodes.RC_400;
import static ke.co.interviewusercaseworld.commons.utils.Helpers.getDefaultRequestHeaderObject;

@Service
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeService {

    private final ProductFeeRepository productFeeRepository;
    private final LoanProductService productService;
    private final ProductFeeMapper productFeeMapper;

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, FeeResponseDto>> addFee(UUID productId, GenericRequest<DefaultRequestHeader, FeeRequestDto> request) {
        return productService.getLoanProduct(productId, Map.of())
                .flatMap(res -> {
                    if (!res.getHeader().getResponseCode().equals(RC_200)) {
                        return Mono.just(GenericResponse.<DefaultResponseHeader, FeeResponseDto>builder()
                                .header(DefaultResponseHeader.builder()
                                        .responseCode(RC_400)
                                        .responseRefId(request.getHeader().getRequestRefId())
                                        .correlationId(request.getHeader().getCorrelationId())
                                        .sourceSystem(request.getHeader().getSourceSystem())
                                        .customerMessage("Loan product not found")
                                        .debugMessage("Loan product could not be found")
                                        .operation(request.getHeader().getOperation())
                                        .build())
                                .build());
                    }
                    LoanProductCreationResponseDto body = res.getBody();
                    return productFeeRepository.existsByProductIdAndFeeType(body.getId(), request.getBody().getFeeType())
                            .switchIfEmpty(Mono.just(false))
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.just(GenericResponse.<DefaultResponseHeader, FeeResponseDto>builder()
                                            .header(DefaultResponseHeader.builder()
                                                    .responseCode(RC_400)
                                                    .responseRefId(request.getHeader().getRequestRefId())
                                                    .correlationId(request.getHeader().getCorrelationId())
                                                    .sourceSystem(request.getHeader().getSourceSystem())
                                                    .customerMessage("Fee already exists")
                                                    .debugMessage("Fee already exists")
                                                    .build())
                                            .build());
                                }
                                return productFeeRepository.save(productFeeMapper.toEntity(request.getBody()))
                                        .map(productFeeMapper::toDto)
                                        .map(feeResponseDto -> {
                                            Helpers.log(request.getHeader().getRequestRefId(), LogLevelEnum.info, OperationNameEnum.PRODUCT_CREATION, "Fee added successfully", null);
                                            return GenericResponse.<DefaultResponseHeader, FeeResponseDto>builder()
                                                    .header(DefaultResponseHeader.builder()
                                                            .responseCode(RC_200)
                                                            .responseRefId(request.getHeader().getRequestRefId())
                                                            .correlationId(request.getHeader().getCorrelationId())
                                                            .sourceSystem(request.getHeader().getSourceSystem())
                                                            .build())
                                                    .body(feeResponseDto)
                                                    .build();
                                        });
                            });
                });
    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, List<FeeResponseDto>>> getFees(UUID productId, Map<String, String> headers) {
        DefaultRequestHeader defaultRequestHeaderObject = getDefaultRequestHeaderObject(headers);
        return productFeeRepository.findByProductId(productId)
                .collectList()
                .defaultIfEmpty(List.of())
                .map(productFees -> {
                    if (productFees.isEmpty()) {
                        return GenericResponse.<DefaultResponseHeader, List<FeeResponseDto>>builder()
                                .header(DefaultResponseHeader.builder()
                                        .sourceSystem(defaultRequestHeaderObject.getSourceSystem())
                                        .correlationId(defaultRequestHeaderObject.getCorrelationId())
                                        .responseRefId(defaultRequestHeaderObject.getRequestRefId())
                                        .responseCode(RC_400)
                                        .customerMessage("No fees found")
                                        .debugMessage("No fees found")
                                        .operation(defaultRequestHeaderObject.getOperation())
                                        .build())
                                .build();
                    }

                    return GenericResponse.<DefaultResponseHeader, List<FeeResponseDto>>builder()
                            .header(DefaultResponseHeader.builder()
                                    .sourceSystem(defaultRequestHeaderObject.getSourceSystem())
                                    .correlationId(defaultRequestHeaderObject.getCorrelationId())
                                    .responseRefId(defaultRequestHeaderObject.getRequestRefId())
                                    .responseCode(RC_200)
                                    .operation(defaultRequestHeaderObject.getOperation())
                                    .build())
                            .body(productFees.stream().map(productFeeMapper::toDto).toList())
                            .build();
                });
    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, Void>> deleteFee(Map<String, String> headers, UUID feeId) {
        DefaultRequestHeader defaultRequestHeaderObject = getDefaultRequestHeaderObject(headers);
        return productFeeRepository.findById(feeId)
                .defaultIfEmpty(new ProductFee())
                .onErrorResume(throwable -> {
                    Helpers.log(defaultRequestHeaderObject.getRequestRefId(), LogLevelEnum.error, defaultRequestHeaderObject.getOperation(), "Error deleting fee", new RuntimeException(throwable));
                    return Mono.just(new ProductFee());
                })
                .flatMap(productFee -> {
                    if (!productFee.getFeeId().equals(feeId)) {
                        return Mono.just(GenericResponse.<DefaultResponseHeader, Void>builder()
                                .header(DefaultResponseHeader.builder()
                                        .sourceSystem(defaultRequestHeaderObject.getSourceSystem())
                                        .correlationId(defaultRequestHeaderObject.getCorrelationId())
                                        .responseRefId(defaultRequestHeaderObject.getRequestRefId())
                                        .responseCode(RC_400)
                                        .customerMessage("Fee not found")
                                        .debugMessage("Fee not found")
                                        .operation(defaultRequestHeaderObject.getOperation())
                                        .build())
                                .build());
                    }
                    return productFeeRepository.delete(productFee)
                            .flatMap(it -> {
                                Helpers.log(defaultRequestHeaderObject.getRequestRefId(), LogLevelEnum.info, defaultRequestHeaderObject.getOperation(), "Fee deleted successfully", null);
                                return Mono.just(GenericResponse.<DefaultResponseHeader, Void>builder()
                                        .header(DefaultResponseHeader.builder()
                                                .sourceSystem(defaultRequestHeaderObject.getSourceSystem())
                                                .correlationId(defaultRequestHeaderObject.getCorrelationId())
                                                .responseRefId(defaultRequestHeaderObject.getRequestRefId())
                                                .responseCode(RC_200)
                                                .operation(defaultRequestHeaderObject.getOperation())
                                                .customerMessage("Fee deleted successfully")
                                                .debugMessage("Fee deleted successfully")
                                                .build())
                                        .build());
                            });
                });
    }
}
