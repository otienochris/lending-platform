package ke.co.expd.authserver.controller;

import ke.co.expd.authserver.model.dto.request.UserCreationRequestDto;
import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import ke.co.expd.authserver.service.UserService;
import ke.co.interviewusercaseworld.commons.dto.requests.DefaultRequestHeader;
import ke.co.interviewusercaseworld.commons.dto.requests.GenericRequest;
import ke.co.interviewusercaseworld.commons.dto.responses.DefaultResponseHeader;
import ke.co.interviewusercaseworld.commons.dto.responses.GenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    @PostMapping
    public Mono<ResponseEntity<GenericResponse<DefaultResponseHeader, UserResponseDto>>> register(@RequestBody GenericRequest<DefaultRequestHeader, UserCreationRequestDto> request ) {

        return userService.register(request)
                .map(res -> {
                    if (res.getHeader().getResponseCode().name().startsWith("RC_2")){
                        return ResponseEntity.ok(res);
                    } else {
                        return ResponseEntity.badRequest().body(res);
                    }
                });
    }
}
