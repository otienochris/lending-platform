package ke.co.interviewusercaseworld.repayment.services.impl;

import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.repayment.services.FundTransfer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class BankAccountFundTransferService implements FundTransfer {
    @Override
    public Mono<Boolean> send(BigDecimal amount, String walletId) {
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.DISBURSEMENT_TO_WALLET, "Sending money to bank wallet: " + walletId, null);
        return Mono.just(true);
    }

    @Override
    public Mono<Boolean> receive(BigDecimal amount, String walletId) {
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.DISBURSEMENT_TO_WALLET, "Receiving money from bank wallet: " + walletId, null);
        return Mono.just(true);
    }
}
