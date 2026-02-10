package ke.co.interviewusercaseworld.repayment.controller;

import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.LoanStatusEnum;
import ke.co.interviewusercaseworld.repayment.model.dto.response.LoanQueryResponseDto;
import ke.co.interviewusercaseworld.repayment.services.LoanRepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoansController {

    private final LoanRepaymentService loanRepaymentService;

    @GetMapping
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, List<LoanQueryResponseDto>>>> queryActiveLoans(@RequestParam UUID userId, @RequestParam(required = false) LoanStatusEnum loanStatus) {

        return loanRepaymentService.queryLoans(userId, loanStatus)
                .flatMap(res -> {
                    if (res.getHeader().getResponseCode().name().startsWith("RC_2")) {
                        return Mono.just(ResponseEntity.ok(res));
                    }
                    return Mono.just(ResponseEntity.badRequest().body(res));
                });

    }
}
