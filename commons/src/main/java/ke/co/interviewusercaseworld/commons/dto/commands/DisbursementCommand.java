package ke.co.interviewusercaseworld.commons.dto.commands;

import ke.co.interviewusercaseworld.commons.enums.WalletTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DisbursementCommand {
    private UUID commandId;
    private UUID loanId;
    private UUID customerId;
    private BigDecimal loanAmount;
    private UUID productId;
    private WalletTypeEnum destinationWallet;
    private String walletId;
    private String loanPurpose;

    //{"loanAmount":500,"productId":"95fcc421-298c-4f11-a98a-38c50feb9ef5","customerId":"d5db7422-bc8b-4a98-b30f-a4475a7069d1","loanPurpose":"Development","tenure":3}
}
