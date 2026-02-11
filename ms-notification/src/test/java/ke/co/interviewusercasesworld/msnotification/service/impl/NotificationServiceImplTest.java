package ke.co.interviewusercasesworld.msnotification.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Test
    void testTemplateParamSubstitution_success(){
        String payload = NotificationServiceImpl.getPayload("Dear {{NAME}}, we have received your request", Map.of("NAME", "John"));

        assertThat(payload).contains("John");
    }

    @Test
    void testTemplateParamSubstitution_missing(){
        String payload = NotificationServiceImpl.getPayload("Dear {{NAME}}, we have received your request", Map.of());

        assertThat(payload).contains("{{NAME}}");
    }

}