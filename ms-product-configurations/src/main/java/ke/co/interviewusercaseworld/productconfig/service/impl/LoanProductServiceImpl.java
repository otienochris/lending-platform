package ke.co.interviewusercaseworld.productconfig.service.impl;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.entities.product.LoanProduct;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.productconfig.mappers.LoanProductMapper;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.LoanProductCreationRequest;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.LoanProductCreationResponseDto;
import ke.co.interviewusercaseworld.productconfig.repository.LoanProductRepository;
import ke.co.interviewusercaseworld.productconfig.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static ke.co.interviewusercaseworld.commons.enums.ResponseCodes.*;
import static ke.co.interviewusercaseworld.commons.utils.Helpers.getDefaultRequestHeaderObject;

@Service
@RequiredArgsConstructor
public class LoanProductServiceImpl implements LoanProductService {

    private final LoanProductMapper loanProductMapper;
    private final LoanProductRepository loanProductRepository;

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>> createLoanProduct(GenericRequest<DefaultRequestHeader, LoanProductCreationRequest> request) {
        String requestRefId = request.getHeader().getRequestRefId();
        Helpers.log(requestRefId, LogLevelEnum.info, OperationNameEnum.PRODUCT_CREATION, "Creating new loan product", null);
        LoanProduct entity = loanProductMapper.toEntity(request.getBody());
        return loanProductRepository.existsByProductName(request.getBody().getProductName())
                .defaultIfEmpty(false)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(GenericResponse.<DefaultResponseHeader, LoanProductCreationResponseDto>builder()
                                        .header(DefaultResponseHeader.builder()
                                                .responseCode(RC_400)
                                                .responseRefId(requestRefId)
                                                .correlationId(request.getHeader().getCorrelationId())
                                                .sourceSystem(request.getHeader().getSourceSystem())
                                                .customerMessage("Loan product already exists")
                                                .debugMessage("Loan product already exists")
                                                .operation(request.getHeader().getOperation())
                                                .build())
                                .build());
                    }
                    return loanProductRepository.save(entity)
                            .switchIfEmpty(Mono.just(LoanProduct.builder().build()))
                            .onErrorResume(e -> {
                                e.printStackTrace();
                                Helpers.log(requestRefId, LogLevelEnum.error, OperationNameEnum.PRODUCT_CREATION, "Error creating product", new RuntimeException(e));
                                return Mono.just(LoanProduct.builder().build());
                            })
                            .map(loanProductMapper::toDto)
                            .map(loanProductCreationResponseDto -> {
                                if (loanProductCreationResponseDto.getId() == null) {
                                    return GenericResponse.<DefaultResponseHeader, LoanProductCreationResponseDto>builder()
                                            .header(DefaultResponseHeader.builder()
                                                    .responseRefId(requestRefId)
                                                    .responseCode(RC_500)
                                                    .operation(request.getHeader().getOperation())
                                                    .correlationId(request.getHeader().getCorrelationId())
                                                    .sourceSystem(request.getHeader().getSourceSystem())
                                                    .customerMessage("Error creating loan product")
                                                    .debugMessage("Loan product could not be created")
                                                    .build())
                                            .build();
                                }
                                Helpers.log(requestRefId, LogLevelEnum.info, OperationNameEnum.PRODUCT_CREATION, "Loan product created successfully", null);
                                DefaultResponseHeader header = DefaultResponseHeader.builder()
                                        .responseCode(RC_200)
                                        .responseRefId(requestRefId)
                                        .correlationId(request.getHeader().getCorrelationId())
                                        .sourceSystem(request.getHeader().getSourceSystem())
                                        .operation(request.getHeader().getOperation())
                                        .customerMessage("Loan product created successfully")
                                        .debugMessage("Loan product created successfully")
                                        .build();
                                return GenericResponse.<DefaultResponseHeader, LoanProductCreationResponseDto>builder()
                                        .header(header)
                                        .body(loanProductCreationResponseDto)
                                        .build();
                            });
                });

    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>> updateLoanProduct(UUID loanProductId, GenericRequest<DefaultRequestHeader, LoanProductCreationRequest> loanProductCreationRequest) {
        return Mono.just(GenericResponse.<DefaultResponseHeader, LoanProductCreationResponseDto>builder().build()); //todo
    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, Void>> deleteLoanProduct(Map<String, String> header, UUID loanProductId) {
        DefaultRequestHeader headers = getDefaultRequestHeaderObject(header);
        Helpers.log(headers.getRequestRefId(), LogLevelEnum.info, headers.getOperation(), "Deleting loan product", null);
        return loanProductRepository.findById(loanProductId)
                .switchIfEmpty(Mono.just(LoanProduct.builder().build()))
                .onErrorResume(throwable -> {
                    Helpers.log(headers.getRequestRefId(), LogLevelEnum.error, headers.getOperation(), "Error deleting loan product", new RuntimeException(throwable));
                    return Mono.just(LoanProduct.builder().build());
                })
                .flatMap(loanProduct -> {
                    if (loanProduct.getId() == null) {
                        Helpers.log(headers.getRequestRefId(), LogLevelEnum.warn, headers.getOperation(), "Loan product not found", null);
                        return Mono.just(GenericResponse.<DefaultResponseHeader, Void>builder()
                                        .header(DefaultResponseHeader.builder()
                                                .responseCode(RC_400)
                                                .responseRefId(headers.getRequestRefId())
                                                .correlationId(headers.getCorrelationId())
                                                .sourceSystem(headers.getSourceSystem())
                                                .customerMessage("Loan product not found")
                                                .debugMessage("Loan product could not be deleted")
                                                .build())
                                .build());
                    }
                    return loanProductRepository.delete(loanProduct)
                            .then(Mono.defer(() -> {
                                Helpers.log(headers.getRequestRefId(), LogLevelEnum.info, headers.getOperation(), "Loan product deleted successfully", null);
                                return Mono.just(GenericResponse.<DefaultResponseHeader, Void>builder()
                                        .header(DefaultResponseHeader.builder()
                                                .responseCode(RC_200)
                                                .responseRefId(headers.getRequestRefId())
                                                .correlationId(headers.getCorrelationId())
                                                .sourceSystem(headers.getSourceSystem())
                                                .customerMessage("Loan product deleted successfully")
                                                .debugMessage("Loan product deleted successfully")
                                                .build())
                                        .build());
                            }));
                });
    }

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>> getLoanProduct(UUID loanProductId, Map<String, String> headers) {
        DefaultRequestHeader headerObject = getDefaultRequestHeaderObject(headers);
        return loanProductRepository.findById(loanProductId)
                .switchIfEmpty(Mono.just(LoanProduct.builder().build()))
                .onErrorResume(throwable -> {
                    Helpers.log(headerObject.getRequestRefId(), LogLevelEnum.error, headerObject.getOperation(), "Error deleting loan product", new RuntimeException(throwable));
                    return Mono.just(LoanProduct.builder().build());
                })
                .flatMap(loanProduct -> {
                    if (loanProduct.getId() == null) {
                        Helpers.log(headerObject.getRequestRefId(), LogLevelEnum.warn, headerObject.getOperation(), "Loan product not found", null);
                        return Mono.just(GenericResponse.<DefaultResponseHeader, LoanProductCreationResponseDto>builder()
                                .header(DefaultResponseHeader.builder()
                                        .responseCode(RC_400)
                                        .responseRefId(headerObject.getRequestRefId())
                                        .correlationId(headerObject.getCorrelationId())
                                        .sourceSystem(headerObject.getSourceSystem())
                                        .customerMessage("Loan product not found")
                                        .debugMessage("Loan product could not be deleted")
                                        .build())
                                .build());
                    }
                    Helpers.log(headerObject.getRequestRefId(), LogLevelEnum.info, headerObject.getOperation(), "Loan product found successfully", null);
                    LoanProductCreationResponseDto dto = loanProductMapper.toDto(loanProduct);
                    return Mono.just(GenericResponse.<DefaultResponseHeader, LoanProductCreationResponseDto>builder()
                                    .header(DefaultResponseHeader.builder()
                                            .responseCode(RC_200)
                                            .responseRefId(headerObject.getRequestRefId())
                                            .correlationId(headerObject.getCorrelationId())
                                            .sourceSystem(headerObject.getSourceSystem())
                                            .operation(headerObject.getOperation())
                                            .build())
                                    .body(dto)
                            .build());
                });
    }

    @Override
    public Flux<LoanProductCreationResponseDto> getAllLoanProducts(Map<String, String> headers) {
        DefaultRequestHeader defaultRequestHeaderObject = getDefaultRequestHeaderObject(headers);
        Helpers.log(defaultRequestHeaderObject.getRequestRefId(), LogLevelEnum.info, defaultRequestHeaderObject.getOperation(), "Creating new loan product", null);
        return loanProductRepository.findAll()
                .map(loanProductMapper::toDto);
    }


}
