package ke.co.interviewusercaseworld.disbursement.service.impl;

import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.enums.WalletTypeEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.disbursement.model.dto.requests.DisbursementRequest;
import ke.co.interviewusercaseworld.disbursement.model.dto.response.DisbursementResponse;
import ke.co.interviewusercaseworld.disbursement.model.entities.LoanDisbursement;
import ke.co.interviewusercaseworld.disbursement.repository.LoanDisbursementRepository;
import ke.co.interviewusercaseworld.disbursement.service.FundTransfer;
import ke.co.interviewusercaseworld.disbursement.service.LoanDisbursementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanDisbursementServiceImpl implements LoanDisbursementService {

    private final LoanDisbursementRepository loanDisbursementRepository;
    private final Map<String, FundTransfer> fundTransferService;


    @Override
    public Mono<GenericResponse<DefaultResponseHeader, DisbursementResponse>> disburse(GenericRequest<DefaultResponseHeader, DisbursementRequest> request) {

        Helpers.log("loan.disbursement.command", LogLevelEnum.INFO, OperationNameEnum.LOAN_DISBURSEMENT, "Handling loan disbursement request", null);
        String name = "";
        try{
            name = request.getBody().getDestinationWallet().name();
        } catch (Exception e){
            Helpers.log("loan.disbursement.command", LogLevelEnum.ERROR, OperationNameEnum.LOAN_DISBURSEMENT, "Error parsing wallet type", e);
        }

        FundTransfer chosenFundTransfer = getChosenFundTransfer(name);

        LoanDisbursement disbursement = LoanDisbursement.builder()
                .customerId(request.getBody().getCustomerId())
                .loanId(request.getBody().getLoanId())
                .status("SUCCESS")
                .productId(request.getBody().getProductId())
                .amount(request.getBody().getAmount())
                .createdAt(LocalDateTime.now())
                .referenceNo(UUID.randomUUID().toString())
                .updatedAt(LocalDateTime.now())
                .build();

        if (chosenFundTransfer == null) {

            disbursement.setStatus("FAILED");
            return loanDisbursementRepository.save(disbursement).flatMap(loanDisbursement -> {
                return Mono.just(GenericResponse.<DefaultResponseHeader, DisbursementResponse>builder()
                        .header(DefaultResponseHeader.builder()
                                .operation(OperationNameEnum.LOAN_DISBURSEMENT)
                                .responseCode(ResponseCodes.RC_400)
                                .customerMessage("Invalid fund transfer service")
                                .debugMessage("Fund transfer service not found: Valid choices include" + Arrays.toString(WalletTypeEnum.values()))
                                .responseRefId(request.getHeader().getResponseRefId())
                                .sourceSystem(request.getHeader().getSourceSystem())
                                .correlationId(request.getHeader().getCorrelationId())
                                .build())
                        .build());
            });

        }

        return chosenFundTransfer.send(request.getBody().getAmount(), request.getBody().getWalletId())
                .flatMap(isSent -> {

                    if (isSent) {
                        return loanDisbursementRepository.save(disbursement)
                                .flatMap(loanDisbursement -> {
                                    Helpers.log("loan.disbursement.command", LogLevelEnum.INFO, OperationNameEnum.LOAN_DISBURSEMENT, "Funds successfully sent to wallet", null);
                                    return Mono.just(GenericResponse.<DefaultResponseHeader, DisbursementResponse>builder()
                                                    .header(DefaultResponseHeader.builder()
                                                            .operation(OperationNameEnum.LOAN_DISBURSEMENT)
                                                            .responseCode(ResponseCodes.RC_200)
                                                            .customerMessage("Funds successfully sent")
                                                            .debugMessage("Funds successfully sent to wallet")
                                                            .responseRefId(request.getHeader().getResponseRefId())
                                                            .sourceSystem(request.getHeader().getSourceSystem())
                                                            .correlationId(request.getHeader().getCorrelationId())
                                                            .responseRefId(loanDisbursement.getDisbursementId().toString())
                                                            .build())
                                                    .body(DisbursementResponse.builder()
                                                            .referenceId(loanDisbursement.getDisbursementId().toString())
                                                            .build())
                                            .build());
                                });
                    }
                    disbursement.setStatus("FAILED");
                    return loanDisbursementRepository.save(disbursement).flatMap(loanDisbursement -> {
                        return Mono.just(GenericResponse.<DefaultResponseHeader, DisbursementResponse>builder()
                                .header(DefaultResponseHeader.builder()
                                        .operation(OperationNameEnum.LOAN_DISBURSEMENT)
                                        .responseCode(ResponseCodes.RC_400)
                                        .customerMessage("Error occurred while sending funds")
                                        .debugMessage("Error occurred while sending funds to wallet")
                                        .responseRefId(loanDisbursement.getDisbursementId().toString())
                                        .sourceSystem(request.getHeader().getSourceSystem())
                                        .correlationId(request.getHeader().getCorrelationId())
                                        .build())
                                .build());
                    });

                });
    }

    private FundTransfer getChosenFundTransfer(String name) {
        System.out.println("Available Fund transfer services:");
        fundTransferService.keySet().forEach(System.out::println);

        String key = name + "FundTransferService";
        StringBuilder sb = new StringBuilder(key);
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        System.out.println("Chosen fund transfer service: " + sb);
        return fundTransferService.get(sb.toString());
    }
}
