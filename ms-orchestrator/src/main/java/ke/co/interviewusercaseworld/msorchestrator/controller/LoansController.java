package ke.co.interviewusercaseworld.msorchestrator.controller;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.dto.responses.UserValidationResponse;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationAcknowledgement;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationResponse;
import ke.co.interviewusercaseworld.msorchestrator.service.LoanProductService;
import ke.co.interviewusercaseworld.msorchestrator.service.LoanService;
import ke.co.interviewusercaseworld.msorchestrator.service.SecurityService;
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

    private final LoanService loanService;

    @PostMapping
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>>> apply(
            @RequestBody GenericRequest<DefaultRequestHeader, LoanApplicationRequest> request) {



        return loanService.apply(request)

                .flatMap(res -> {
                    if (res.getHeader().getResponseCode().name().startsWith("RC_2")){
                        return Mono.just(ResponseEntity.ok(res));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(res));
                    }
                });

    }
}
