package ke.co.expd.authserver.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Table(schema = "auth", name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    private UUID userId;
    private String token;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("is_revoked")
    private boolean isRevoked;

    @Builder.Default
    @Column("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
