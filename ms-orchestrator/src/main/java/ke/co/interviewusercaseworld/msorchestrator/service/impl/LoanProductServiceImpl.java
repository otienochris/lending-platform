package ke.co.interviewusercaseworld.msorchestrator.service.impl;

import ke.co.interviewusercaseworld.commons.configs.AppProperties;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.msorchestrator.configs.OrchestratorProperties;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.AuthResponse;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationResponse;
import ke.co.interviewusercaseworld.msorchestrator.service.LoanProductService;
import ke.co.interviewusercaseworld.msorchestrator.utils.GlobalHelpers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.getDefaultRequestHeaderObject;

@Service
@RequiredArgsConstructor
public class LoanProductServiceImpl implements LoanProductService {

    private final WebClient webClient;
    private final OrchestratorProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<GenericResponse<DefaultResponseHeader, LoanApplicationResponse>> validateProduct(Map<String, String> headers, GenericRequest<DefaultRequestHeader, LoanApplicationRequest> request) {

        DefaultRequestHeader defaultRequestHeaderObject = getDefaultRequestHeaderObject(headers);
        String requestRefId = request.getHeader().getRequestRefId();
        Helpers.log(requestRefId, LogLevelEnum.info, OperationNameEnum.LOAN_APPLICATION, "initiating loan application", null);
        // validate product
        String body = "";

        AppProperties.ServiceSetup productConfigService = appProperties.getServiceProperties().getOrchestratorProperties().getExternalMicroServices().getOrDefault("ms-product-configuration", AppProperties.ServiceSetup.builder().build());

        AppProperties.AuthApiSpec authSpec = productConfigService.getAuth();
        AppProperties.ApiSpec productValidationApiSpec = productConfigService.getProductValidationApiSpec();


        boolean isProtected = productConfigService.getProductValidationApiSpec().isProtected();

        return GlobalHelpers.executeAuthRequest(requestRefId, request.getHeader().getOperation(), headers, body, authSpec, webClient)
                .flatMap(s -> {
                    AuthResponse authResponse = objectMapper.readValue(s, AuthResponse.class);
                    if (authResponse == null || authResponse.getAccessToken() == null) {
                        return Mono.just(GenericResponse.<DefaultResponseHeader, LoanApplicationResponse>builder()
                                .header(DefaultResponseHeader.builder()
                                        .responseRefId(requestRefId)
                                        .correlationId(request.getHeader().getCorrelationId())
                                        .sourceSystem(request.getHeader().getSourceSystem())
                                        .customerMessage("Authentication failed")
                                        .debugMessage("Authentication failed")
                                        .responseCode(ResponseCodes.RC_401)
                                        .build())
                                .build());
                    }

                    String accessToken = authResponse.getAccessToken();
                    if (isProtected) {
                        headers.put("Authorization", "Bearer " + accessToken);
                    }

                    return GlobalHelpers.executeRequest(requestRefId, request.getHeader().getOperation(), headers, body, productValidationApiSpec, webClient)
                            .map(s1 -> objectMapper.readValue(s1, LoanApplicationResponse.class))
                            .onErrorResume(error -> {
                                Helpers.log(requestRefId, LogLevelEnum.ERROR, request.getHeader().getOperation(), "Error validating product", new RuntimeException(error));
                                return Mono.just(LoanApplicationResponse.builder().build());
                            })
                            .defaultIfEmpty(LoanApplicationResponse.builder().build())
                            .map(loanApplicationResponse -> {
                                DefaultResponseHeader header = DefaultResponseHeader.builder().build();
                                header.setResponseRefId(requestRefId);
                                header.setCorrelationId(request.getHeader().getCorrelationId());
                                header.setSourceSystem(request.getHeader().getSourceSystem());

                                if (loanApplicationResponse.getLoanId() == null) {
                                    Helpers.log(requestRefId, LogLevelEnum.ERROR, request.getHeader().getOperation(), "Loan product not found", new RuntimeException("Loan product not found"));
                                    header.setResponseCode(ResponseCodes.RC_400);
                                    header.setCustomerMessage("Loan product not found");
                                    header.setDebugMessage("Loan product not found");

                                } else {
                                    Helpers.log(requestRefId, LogLevelEnum.INFO, request.getHeader().getOperation(), "Loan product is valid", null);
                                    header.setResponseCode(ResponseCodes.RC_200);
                                    header.setCustomerMessage("Loan product is valid");
                                    header.setDebugMessage("Loan product is valid");
                                }
                                GenericResponse<DefaultResponseHeader, LoanApplicationResponse> res = GenericResponse.<DefaultResponseHeader, LoanApplicationResponse>builder()
                                        .header(header).build();
                                return res;
                            });
                });

    }
}
