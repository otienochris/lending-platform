package ke.co.interviewusercaseworld.msorchestrator.controller;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationResponse;
import ke.co.interviewusercaseworld.msorchestrator.service.LoanProductService;
import ke.co.interviewusercaseworld.msorchestrator.utils.GlobalHelpers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static ke.co.interviewusercaseworld.commons.utils.Helpers.getDefaultRequestHeaderObject;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoansController {

    private final WebClient webClient;
    private final LoanProductService loanProductService;
    private final LoanProductService loanProductService;

    @PostMapping
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanApplicationResponse>>> apply(@RequestHeader Map<String, String> headers,
                                                                                                       @RequestBody GenericRequest<DefaultRequestHeader, LoanApplicationRequest> request) {

        Mono<GenericResponse<DefaultResponseHeader, LoanApplicationResponse>> response = loanProductService.validateProduct(headers,request);




        // validate user
        // create saga
        // command disbursement service

    }
}
