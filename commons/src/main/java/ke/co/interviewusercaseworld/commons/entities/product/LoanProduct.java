package ke.co.interviewusercaseworld.commons.entities.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@Table(schema = "product_configs", value = "loan_products")
public class LoanProduct {

    @Id
    private UUID id;
    private String productName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private String tenureOptions;
    private Boolean active;
}
