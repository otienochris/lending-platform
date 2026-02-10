package ke.co.interviewusercaseworld.productconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsProductConfigurationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsProductConfigurationsApplication.class, args);
    }

}
