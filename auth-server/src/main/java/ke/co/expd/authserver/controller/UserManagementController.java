package ke.co.expd.authserver.controller;

import ke.co.expd.authserver.model.dto.request.GenericRequest;
import ke.co.expd.authserver.model.dto.request.UserCreationRequestDto;
import ke.co.expd.authserver.model.dto.response.GenericResponse;
import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import ke.co.expd.authserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigInteger;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    @PostMapping
    public Mono<ResponseEntity<GenericResponse<UserResponseDto>>> register(@RequestBody GenericRequest<UserCreationRequestDto> request ) {

        return userService.register(request)
                .map(res -> {
                    if (res.getHeader().getStatus().startsWith("RC_2")){
                        return ResponseEntity.ok(res);
                    } else {
                        return ResponseEntity.badRequest().body(res);
                    }
                });
    }
}
