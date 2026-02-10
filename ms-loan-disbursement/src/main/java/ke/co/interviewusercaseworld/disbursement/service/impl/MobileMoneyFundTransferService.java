package ke.co.interviewusercaseworld.disbursement.service.impl;

import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import ke.co.interviewusercaseworld.disbursement.service.FundTransfer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class MobileMoneyFundTransferService implements FundTransfer {
    @Override
    public Mono<Boolean> send(BigDecimal amount, String walletId) {
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.DISBURSEMENT_TO_WALLET, "Sending money to mobile wallet: " + walletId, null);
        return Mono.just(true);
    }
}
