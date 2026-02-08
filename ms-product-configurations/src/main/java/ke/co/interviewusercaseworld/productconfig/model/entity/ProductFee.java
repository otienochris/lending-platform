package ke.co.interviewusercaseworld.productconfig.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "product_configs", value = "fees")
public class ProductFee {
    @Id
    private UUID feeId;

    private UUID productId;
    private String feeType;
    private BigDecimal feeAmount;
}
