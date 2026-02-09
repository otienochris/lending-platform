package ke.co.interviewusercaseworld.productconfig.controller;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.FeeRequestDto;
import ke.co.interviewusercaseworld.productconfig.model.dto.request.LoanProductCreationRequest;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.FeeResponseDto;
import ke.co.interviewusercaseworld.productconfig.model.dto.response.LoanProductCreationResponseDto;
import ke.co.interviewusercaseworld.productconfig.service.FeeService;
import ke.co.interviewusercaseworld.productconfig.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loan-products")
@RequiredArgsConstructor
public class LoanProductController {

    private final LoanProductService productService;
    private final FeeService feeService;

    @PostMapping
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>>> create(
            @RequestBody GenericRequest<DefaultRequestHeader, LoanProductCreationRequest> request
    ) {

        return productService.createLoanProduct(request)
                .flatMap(response -> {
                    if (ResponseCodes.RC_200.equals(response.getHeader().getResponseCode())){
                        return Mono.just(ResponseEntity.ok(response));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(response));
                    }
                });
    }

    @GetMapping
    public Flux<LoanProductCreationResponseDto> getAll(@RequestHeader Map<String, String> headers) {
        return productService.getAllLoanProducts(headers);
    }

    @GetMapping("/{productId}")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>>> getById(
            @PathVariable UUID productId,
            @RequestHeader Map<String, String> headers
    ) {
        return productService.getLoanProduct(productId, headers)
                .flatMap(response -> {
                    if (ResponseCodes.RC_200.equals(response.getHeader().getResponseCode())){
                        return Mono.just(ResponseEntity.ok(response));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(response));
                    }
                });
    }

    @PutMapping("/{productId}")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanProductCreationResponseDto>>> update(
            @PathVariable UUID productId,
            @RequestBody GenericRequest<DefaultRequestHeader, LoanProductCreationRequest> request
    ) {
        return productService.updateLoanProduct(productId, request)
                .flatMap(response -> {
                    if (ResponseCodes.RC_200.equals(response.getHeader().getResponseCode())){
                        return Mono.just(ResponseEntity.ok(response));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(response));
                    }
                });
    }

    @DeleteMapping("/{productId}")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, Void>>> delete(@PathVariable UUID productId,
                             @RequestHeader Map<String, String> headers) {
        return productService.deleteLoanProduct(headers, productId)
                .flatMap(res -> {
                    if (ResponseCodes.RC_200.equals(res.getHeader().getResponseCode())){
                        return Mono.just(ResponseEntity.ok(res));
                    }
                    return Mono.just(ResponseEntity.badRequest().body(res));
                });
    }

    // -------- FEES --------

    @PostMapping("/{productId}/fees")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, FeeResponseDto>>> addFee(
            @PathVariable UUID productId,
            @RequestBody GenericRequest<DefaultRequestHeader, FeeRequestDto> request
    ) {
        return feeService.addFee(productId, request)
                .flatMap(response -> {
                    if (ResponseCodes.RC_200.equals(response.getHeader().getResponseCode())){
                        return Mono.just(ResponseEntity.ok(response));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(response));
                    }
                });
    }

    @GetMapping("/{productId}/fees")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, List<FeeResponseDto>>>> getFees(@RequestHeader Map<String, String> headers, @PathVariable UUID productId) {
        return feeService.getFees(productId, headers)
                .flatMap(response -> {
                    if (ResponseCodes.RC_200.equals(response.getHeader().getResponseCode())){
                        return Mono.just(ResponseEntity.ok(response));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(response));
                    }
                });
    }

    @DeleteMapping("/fees/{feeId}")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, Void>>> deleteFee(@RequestHeader Map<String, String> headers, @PathVariable UUID feeId) {
        return feeService.deleteFee(headers, feeId)
                .flatMap(response -> {
                    if (ResponseCodes.RC_200.equals(response.getHeader().getResponseCode())){
                        return Mono.just(ResponseEntity.ok(response));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(response));
                    }
                });
    }
}
