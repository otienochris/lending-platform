package ke.co.interviewusercaseworld.repayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MsLoanRepaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsLoanRepaymentApplication.class, args);
    }

}
