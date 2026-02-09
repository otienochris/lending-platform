package ke.co.expd.authserver.service;

import ke.co.expd.authserver.model.dto.request.GenericRequest;
import ke.co.expd.authserver.model.dto.request.UpdateUserRequest;
import ke.co.expd.authserver.model.dto.request.UserCreationRequestDto;
import ke.co.expd.authserver.model.dto.response.GenericResponse;
import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.UUID;

public interface UserService {
    Flux<UserResponseDto> getAllUsers(BigInteger pageNumber, BigInteger pageSize);

    Mono<UserResponseDto> getUserById(UUID id);

    Mono<UserResponseDto> updateUser(UUID id, UpdateUserRequest request);

    Mono<Boolean> assignRoleToUser(UUID userId, UUID roleId, boolean validateIds);

    Mono<GenericResponse<UserResponseDto>> register(GenericRequest<UserCreationRequestDto> request);
}
