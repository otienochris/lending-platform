package ke.co.interviewusercaseworld.commons.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table(schema = "lending", value = "products")
public class Product {

    @Id
    private BigDecimal id;
}
