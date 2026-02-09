package ke.co.expd.authserver.service.impl;

import ke.co.expd.authserver.config.CustomUserPrincipal;
import ke.co.expd.authserver.exceptions.UserNotFound;
import ke.co.expd.authserver.model.dto.request.GenericRequest;
import ke.co.expd.authserver.model.dto.request.UpdateUserRequest;
import ke.co.expd.authserver.model.dto.request.UserCreationRequestDto;
import ke.co.expd.authserver.model.dto.response.GenericResponse;
import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import ke.co.expd.authserver.model.entities.Role;
import ke.co.expd.authserver.model.entities.User;
import ke.co.expd.authserver.model.entities.UserRole;
import ke.co.expd.authserver.model.entities.UserRoleId;
import ke.co.expd.authserver.repoisitory.RoleRepository;
import ke.co.expd.authserver.repoisitory.UserRepository;
import ke.co.expd.authserver.repoisitory.UserRoleRepository;
import ke.co.expd.authserver.repoisitory.impl.UserRoleCustomRepository;
import ke.co.expd.authserver.service.UserService;
import ke.co.interviewusercaseworld.commons.enums.LogLevelEnum;
import ke.co.interviewusercaseworld.commons.enums.OperationNameEnum;
import ke.co.interviewusercaseworld.commons.enums.ResponseCodes;
import ke.co.interviewusercaseworld.commons.utils.Helpers;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, ReactiveUserDetailsService {

    public static final String DEFAULT_ROLE_NAME = "CUSTOMER";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final DummyCacheManage cacheManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleCustomRepository userRoleCustomRepository;

    private static boolean stringIsPresent(String string) {
        return string != null && !string.isBlank();
    }

    @Override
    public Flux<UserResponseDto> getAllUsers(BigInteger pageNumber, BigInteger pageSize) {
        BigInteger offset = pageNumber.multiply(pageSize);

        return userRepository.findAllByPageable(offset, pageSize)
                .map(this::toUserResponse);
    }

    private UserResponseDto toUserResponse(User user) {
        return UserResponseDto.builder().build();
    }

    @Override
    public Mono<UserResponseDto> getUserById(UUID id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .map(this::toUserResponse);
    }

    @Override
    public Mono<UserResponseDto> updateUser(UUID id, UpdateUserRequest request) {

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFound("User not found")))
                .flatMap(user -> {
                    String currentEmail = request.getEmail();
                    if (stringIsPresent(currentEmail) && !currentEmail.equalsIgnoreCase(user.getEmail())) {
                        return userRepository.existsByEmail(currentEmail)
                                .flatMap(exists -> {
                                    if (exists) {
                                        return Mono.error(new RuntimeException("Email is already in use"));
                                    } else {
                                        user.setEmail(currentEmail);
                                        user.setEmailVerified(false);
                                        return Mono.just(user);
                                    }
                                });
                    }
                    return Mono.just(user);
                })
                .flatMap(user -> {
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .map(this::toUserResponse)
                .doOnSuccess(user -> evictUserCache(id));
    }

    @Override
    public Mono<Boolean> assignRoleToUser(UUID userId, UUID roleId, boolean validateIds) {
        return Mono.zip(
                        !validateIds ? Mono.just(true) : userRepository.existsById(userId),
                        !validateIds ? Mono.just(true) : roleRepository.existsById(roleId)
                )
                .flatMap(results -> {
                    if (!results.getT1()) {
                        return Mono.error(new RuntimeException("User not found"));
                    }

                    if (!results.getT2()) {
                        return Mono.error(new RuntimeException("Role not found"));
                    }

                    return userRoleCustomRepository.saveUserRole(new UserRole(new UserRoleId(userId, roleId)));
                })
                .doOnSuccess(v -> evictUserCache(userId))
                .flatMap(Mono::just);
    }

    @Override
    public Mono<GenericResponse<UserResponseDto>> register(GenericRequest<UserCreationRequestDto> request) {
        return userRepository.existsByUsername(request.getBody().getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(GenericResponse.<UserResponseDto>builder()
                                .header(GenericResponse.Header.builder()
                                        .responseRefId(request.getHeader().getRequestRefId())
                                        .customerMessage("Username is already taken")
                                        .debugMessage("Username is already taken")
                                        .status(ResponseCodes.RC_409.name())
                                        .build())
                                .build());
                    } else {
                        return userRepository.existsByEmail(request.getBody().getEmail())
                                .flatMap(emailExists -> {

                                    if (emailExists) {
                                        return Mono.just(GenericResponse.<UserResponseDto>builder()
                                                .header(GenericResponse.Header.builder()
                                                        .responseRefId(request.getHeader().getRequestRefId())
                                                        .customerMessage("Email is already taken")
                                                        .debugMessage("Email is already taken")
                                                        .status(ResponseCodes.RC_409.name())
                                                        .build())
                                                .build());

                                    }

                                    User newUser = toUserEntity(request.getBody());
                                    return Mono.zip(
                                            userRepository.save(newUser),
                                            roleRepository.findByName(DEFAULT_ROLE_NAME).switchIfEmpty(Mono.just(new Role()))
                                    ).flatMap(tuple -> {
                                        User user = tuple.getT1();
                                        Role role = tuple.getT2();
                                        if (user.getId() == null) {
                                            return Mono.just(GenericResponse.<UserResponseDto>builder()
                                                    .header(GenericResponse.Header.builder()
                                                            .responseRefId(request.getHeader().getRequestRefId())
                                                            .customerMessage("User could not be created")
                                                            .debugMessage("User could not be created")
                                                            .status(ResponseCodes.RC_400.name())
                                                            .build())
                                                    .build());
                                        }

                                        if (role.getId() == null) {
                                            return Mono.just(GenericResponse.<UserResponseDto>builder()
                                                    .header(GenericResponse.Header.builder()
                                                            .responseRefId(request.getHeader().getRequestRefId())
                                                            .customerMessage("Default role is not present")
                                                            .debugMessage("Create role ROLE_USER first")
                                                            .status(ResponseCodes.RC_400.name())
                                                            .build())
                                                    .build());
                                        }
                                        return assignRoleToUser(user.getId(), role.getId(), true)
                                                .flatMap(v -> {
                                                    GenericResponse<UserResponseDto> response = GenericResponse.<UserResponseDto>builder().build();
                                                    if (v.equals(true)) {
                                                        response.setHeader(GenericResponse.Header.builder()
                                                                        .responseRefId(request.getHeader().getRequestRefId())
                                                                        .status(ResponseCodes.RC_201.name())
                                                                        .customerMessage("User created successfully")
                                                                        .debugMessage("User created successfully")
                                                                .build());
                                                    } else {
                                                        response.setHeader(GenericResponse.Header.builder()
                                                                .responseRefId(request.getHeader().getRequestRefId())
                                                                .status(ResponseCodes.RC_200.name())
                                                                .customerMessage("User created successfully but role assignment failed")
                                                                .debugMessage("Use role assignment API to assign role to user")
                                                                .build());
                                                    }
                                                    return Mono.just(response);
                                                });
                                    });

                                });
                    }
                });
    }

    private User toUserEntity(UserCreationRequestDto user) {
        String encoded = passwordEncoder.encode(user.getPassword());
        return User.builder()
                .password(encoded)
                .email(user.getEmail())
                .username(user.getUsername())
                .lastName(user.getLastName())
                .firstName(user.getFirstName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isEmailVerified(false)
                .isEnabled(false)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
    }

    private Boolean evictUserCache(UUID userId) {
        Cache cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict(userId);
        }
        return true;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.USER_DETAILS_RETRIEVAL, "Retrieving user details", null);
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UserNotFound("User not found")))
                .defaultIfEmpty(new User())
                .flatMap(userDetails -> {
                    return userRoleRepository.findByUserId(userDetails.getId())
                            .flatMap(userRoles -> roleRepository.findById(userRoles.getId().getRoleId()))
                            .map(Role::getName)
                            .collectList()
                            .defaultIfEmpty(List.of())
                            .flatMap(roles -> {
                                CustomUserPrincipal userPrincipal = CustomUserPrincipal.builder()
                                        .id(userDetails.getId())
                                        .username(userDetails.getUsername())
                                        .password(userDetails.getPassword())
                                        .email(userDetails.getEmail())
                                        .isEmailVerified(userDetails.isEmailVerified())
                                        .isEnabled(userDetails.isEnabled())
                                        .isAccountNonExpired(userDetails.isAccountNonExpired())
                                        .isCredentialsNonExpired(userDetails.isCredentialsNonExpired())
                                        .isAccountNonLocked(userDetails.isAccountNonLocked())
                                        .roles(roles)
                                        .build();
                                Helpers.log("", LogLevelEnum.INFO, OperationNameEnum.USER_DETAILS_RETRIEVAL, "User details found", null);
                                return Mono.just(userPrincipal);
                            });
                });
    }
}
