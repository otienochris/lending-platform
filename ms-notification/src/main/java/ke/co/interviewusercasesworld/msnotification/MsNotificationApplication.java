package ke.co.interviewusercasesworld.msnotification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsNotificationApplication.class, args);
    }
}
