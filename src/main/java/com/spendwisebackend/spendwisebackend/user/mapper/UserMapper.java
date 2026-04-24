package com.spendwisebackend.spendwisebackend.user.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.spendwisebackend.spendwisebackend.user.models.dto.UserDTO;
import com.spendwisebackend.spendwisebackend.user.models.entities.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    UserDTO toDTO(User user);

    User toEntity(UserDTO dto);

    @Mapping(target = "password", ignore = true)
    List<UserDTO> toDTOList(List<User> users);

    @Mapping(target = "password", ignore = true)
    void updateEntityFromDto(UserDTO dto, @MappingTarget User entity);
}
