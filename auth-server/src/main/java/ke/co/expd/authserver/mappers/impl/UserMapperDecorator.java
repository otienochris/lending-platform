package ke.co.expd.authserver.mappers.impl;

import ke.co.expd.authserver.mappers.UserMapper;
import ke.co.expd.authserver.model.dto.request.UserCreationRequestDto;
import ke.co.expd.authserver.model.dto.response.UserResponseDto;
import ke.co.expd.authserver.model.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class UserMapperDecorator implements UserMapper {

    @Autowired
    @Qualifier("delegate")
    private UserMapper userMapper;

    @Override
    public UserResponseDto toDto(User user) {
        UserResponseDto dto = userMapper.toDto(user);
        return dto;
    }

    @Override
    public User toEntity(UserCreationRequestDto user) {
        User entity = userMapper.toEntity(user);
        return entity;
    }
}
