package ke.co.interviewusercaseworld.msorchestrator.controller;

import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanApplicationAcknowledgement;
import ke.co.interviewusercaseworld.msorchestrator.model.dto.response.LoanRepaymentRequestAck;
import ke.co.interviewusercaseworld.msorchestrator.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoansControllerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoansController loansController;

    private <T> GenericResponse<DefaultResponseHeader, T> successResponse(OperationNameEnum operation, T body) {

        return GenericResponse.<DefaultResponseHeader, T>builder()
                .header(DefaultResponseHeader.builder()
                        .operation(operation)
                        .responseCode(ResponseCodes.RC_200)
                        .responseRefId("ref-123")
                        .customerMessage("Success")
                        .sourceSystem("MS-LOAN-DISBURSEMENT")
                        .build())
                .body(body)
                .build();
    }

    private <T> GenericResponse<DefaultResponseHeader, T> failureResponse(
            OperationNameEnum operation) {

        return GenericResponse.<DefaultResponseHeader, T>builder()
                .header(DefaultResponseHeader.builder()
                        .operation(operation)
                        .responseCode(ResponseCodes.RC_400)
                        .responseRefId("ref-123")
                        .customerMessage("Failure")
                        .sourceSystem("MS-LOAN-DISBURSEMENT")
                        .build())
                .build();
    }

    @Test
    void repay_success_returnsOk() {
        var request = mock(GenericRequest.class);
        var response = successResponse(
                OperationNameEnum.LOAN_REPAYMENT,
                new LoanRepaymentRequestAck()
        );

        when(loanService.repay(request))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanRepaymentRequestAck>>> result =
                loansController.repay(request);

        StepVerifier.create(result)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.OK, entity.getStatusCode());
                    assertEquals(ResponseCodes.RC_200, entity.getBody().getHeader().getResponseCode());
                })
                .verifyComplete();
    }

    @Test
    void repay_failureResponse_returnsBadRequest() {
        var request = mock(GenericRequest.class);
        var response = failureResponse(OperationNameEnum.LOAN_REPAYMENT);

        when(loanService.repay(request))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanRepaymentRequestAck>>> result =
                loansController.repay(request);

        StepVerifier.create(result)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
                    assertEquals(ResponseCodes.RC_400, entity.getBody().getHeader().getResponseCode());
                })
                .verifyComplete();
    }


    @Test
    void repay_exception_returnsBadRequestWithErrorHeader() {
        var request = mock(GenericRequest.class);

        when(loanService.repay(request))
                .thenReturn(Mono.error(new RuntimeException("DB down")));

        Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanRepaymentRequestAck>>> result =
                loansController.repay(request);

        StepVerifier.create(result)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
                    assertEquals(ResponseCodes.RC_400, entity.getBody().getHeader().getResponseCode());
                    assertEquals(
                            "Error occurred while repaying your loan",
                            entity.getBody().getHeader().getCustomerMessage()
                    );
                    assertEquals("DB down", entity.getBody().getHeader().getDebugMessage());
                })
                .verifyComplete();
    }


    @Test
    void apply_success_returnsOk() {
        var request = mock(GenericRequest.class);
        var response = successResponse(
                OperationNameEnum.LOAN_APPLICATION,
                new LoanApplicationAcknowledgement()
        );

        when(loanService.apply(request))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>>> result =
                loansController.apply(request);

        StepVerifier.create(result)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.OK, entity.getStatusCode());
                    assertEquals(ResponseCodes.RC_200, entity.getBody().getHeader().getResponseCode());
                })
                .verifyComplete();
    }

    @Test
    void apply_failureResponse_returnsBadRequest() {
        var request = mock(GenericRequest.class);
        var response = failureResponse(OperationNameEnum.LOAN_APPLICATION);

        when(loanService.apply(request))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>>> result =
                loansController.apply(request);

        StepVerifier.create(result)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
                    assertEquals(ResponseCodes.RC_400, entity.getBody().getHeader().getResponseCode());
                })
                .verifyComplete();
    }


    @Test
    void apply_exception_returnsBadRequestWithErrorHeader() {
        var request = mock(GenericRequest.class);

        when(loanService.apply(request))
                .thenReturn(Mono.error(new RuntimeException("Kafka unavailable")));

        Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, LoanApplicationAcknowledgement>>> result =
                loansController.apply(request);

        StepVerifier.create(result)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
                    assertEquals(ResponseCodes.RC_400, entity.getBody().getHeader().getResponseCode());
                    assertEquals(
                            "Error occurred while applying for loan",
                            entity.getBody().getHeader().getCustomerMessage()
                    );
                    assertEquals("Kafka unavailable", entity.getBody().getHeader().getDebugMessage());
                })
                .verifyComplete();
    }





}