package ke.co.interviewusercaseworld.commons.entities.product;

import ke.co.interviewusercaseworld.commons.enums.InterestRateTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "product_configs", value = "loan_products")
public class LoanProduct {

    @Id
    private UUID id;
    private String productName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private String interestRateType;
    private String tenureOptions;
    private String tenureOptionsType;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String currency;
    private Boolean supportInstallments;

    private Boolean active;
}
