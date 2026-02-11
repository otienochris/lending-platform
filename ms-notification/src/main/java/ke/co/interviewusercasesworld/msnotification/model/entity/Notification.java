package ke.co.interviewusercasesworld.msnotification.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "notifications", value = "notifications")
public class Notification {
    @Id
    @Column("notification_id")
    private UUID id;

    private String msisdn;
    private String emailsTo;
    private String bcc;
    private String cc;
    private String subject;
    private String channel;
    private String templateCode;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    @Builder.Default
    private LocalDateTime sendAt = LocalDateTime.now();
}
