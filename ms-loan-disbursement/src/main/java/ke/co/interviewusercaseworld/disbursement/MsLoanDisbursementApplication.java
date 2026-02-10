package ke.co.interviewusercaseworld.disbursement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsLoanDisbursementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsLoanDisbursementApplication.class, args);
    }

}
