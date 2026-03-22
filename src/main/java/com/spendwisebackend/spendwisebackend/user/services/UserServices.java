package com.spendwisebackend.spendwisebackend.user.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.spendwisebackend.spendwisebackend.user.CustomUserDetails;
import com.spendwisebackend.spendwisebackend.user.models.dto.LoginRequset;
import com.spendwisebackend.spendwisebackend.user.models.dto.UserDTO;
import com.spendwisebackend.spendwisebackend.user.models.entities.User;

public interface UserServices {
    UserDTO getUserById(UUID id, String details);

    UserDTO saveUser(UserDTO user);

    UserDTO updateUser(UserDTO entity);

    void deleteUser(UUID entity);

    List<UserDTO> getAllUsers(Pageable page);

    CustomUserDetails login(LoginRequset data);

    User getUserByEmail(String email);
}
