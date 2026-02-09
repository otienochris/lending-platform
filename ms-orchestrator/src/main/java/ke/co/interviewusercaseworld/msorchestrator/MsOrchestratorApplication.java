package ke.co.interviewusercaseworld.msorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsOrchestratorApplication.class, args);
    }

}
