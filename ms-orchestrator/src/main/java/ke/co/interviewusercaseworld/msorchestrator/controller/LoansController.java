package ke.co.interviewusercaseworld.msorchestrator.controller;

import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanApplicationRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.request.LoanRepaymentRequest;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationAcknowledgement;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanRepaymentRequestAck;
import ke.co.interviewusercaseworld.msorchestrator.repository.CommandDeduplicationRepo;
import ke.co.interviewusercaseworld.msorchestrator.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoansController {

    private final LoanService loanService;
    private final CommandDeduplicationRepo commandDeduplicationRepo;

    @PostMapping("/repayment")
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanRepaymentRequestAck>>> repay(
            @RequestBody GenericRequest<DefaultRequestHeader, LoanRepaymentRequest> request) {

        return loanService.repay(request)
                .flatMap(res -> {
                    if (res.getHeader().getResponseCode().name().startsWith("RC_2")){
                        return Mono.just(ResponseEntity.ok(res));
                    } else {
                        return Mono.just(ResponseEntity.badRequest().body(res));
                    }
                }).onErrorResume(throwable -> Mono.just(ResponseEntity.badRequest().body(GenericResponse.<DefaultResponseHeader, LoanRepaymentRequestAck>builder()
                        .header(DefaultResponseHeader.builder()
                                .operation(OperationNameEnum.LOAN_REPAYMENT)
                                .responseRefId("")
                                .responseCode(ResponseCodes.RC_400)
                                .customerMessage("Error occurred while repaying your loan")
                                .debugMessage(throwable.getMessage())
                                .sourceSystem("MS-LOAN-DISBURSEMENT")
                                .build())
                        .build())));

    }

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
                }).onErrorResume(throwable -> Mono.just(ResponseEntity.badRequest().body(GenericResponse.<DefaultResponseHeader, LoanApplicationAcknowledgement>builder()
                                .header(DefaultResponseHeader.builder()
                                        .operation(OperationNameEnum.LOAN_APPLICATION)
                                        .responseRefId("")
                                        .responseCode(ResponseCodes.RC_400)
                                        .customerMessage("Error occurred while applying for loan")
                                        .debugMessage(throwable.getMessage())
                                        .sourceSystem("MS-LOAN-DISBURSEMENT")
                                        .build())
                        .build())));

    }
}
