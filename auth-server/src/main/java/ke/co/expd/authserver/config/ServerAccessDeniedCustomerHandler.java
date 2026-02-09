package ke.co.expd.authserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.co.expd.authserver.model.dto.response.AuthResponse;
import ke.co.expd.authserver.model.dto.response.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class ServerAccessDeniedCustomerHandler implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return Mono.defer(() -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            GenericResponse<AuthResponse> errorResponse = GenericResponse.<AuthResponse>builder()
                    .header(GenericResponse.Header.builder()
                            .responseRefId(exchange.getRequest().getHeaders().getFirst("X-Request-Id"))
                            .status(HttpStatus.UNAUTHORIZED.name())
                            .customerMessage("Access Denied: You don't have permission to access this resource")
                            .debugMessage(denied.getMessage())
                            .build())
                    .build();

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                byte[] bytes = objectMapper.writeValueAsString(errorResponse)
                        .getBytes(StandardCharsets.UTF_8);

                return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }
}
