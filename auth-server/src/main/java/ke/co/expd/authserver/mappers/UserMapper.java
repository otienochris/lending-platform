package ke.co.expd.authserver.mappers;

import ke.co.expd.authserver.mappers.impl.UserMapperDecorator;
import ke.co.expd.authserver.model.dto.request.UserCreationRequestDto;
import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import ke.co.expd.authserver.model.entities.User;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
@DecoratedWith(UserMapperDecorator.class)
public interface UserMapper {

    UserResponseDto toDto(User user);
    User toEntity(UserCreationRequestDto user);
}
