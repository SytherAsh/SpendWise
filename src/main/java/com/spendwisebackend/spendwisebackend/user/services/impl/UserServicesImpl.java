package com.spendwisebackend.spendwisebackend.user.services.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import com.spendwisebackend.spendwisebackend.user.CustomUserDetails;
import com.spendwisebackend.spendwisebackend.user.mapper.UserMapper;
import com.spendwisebackend.spendwisebackend.user.models.dto.LoginRequset;
import com.spendwisebackend.spendwisebackend.user.models.dto.UserDTO;
import com.spendwisebackend.spendwisebackend.user.models.entities.User;
import com.spendwisebackend.spendwisebackend.user.repositories.UserRepository;
import com.spendwisebackend.spendwisebackend.user.services.UserServices;

import com.spendwisebackend.spendwisebackend.config.Security.LoginAttemptService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServicesImpl implements UserServices {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id, String details) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Not found user by : " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(Pageable pageable) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Page<User> userPage = userRepository.findAll(pageable);
        List<UserDTO> users = userMapper.toDTOList(userPage.getContent());

        stopWatch.stop();
        log.info("time get user: {} ms", stopWatch.getTotalTimeMillis());
        return users;
    }

    @Override
    @Transactional
    public UserDTO saveUser(UserDTO dto) {
        User user = userMapper.toEntity(dto);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setId(null);
        return userMapper.toDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDTO updateUser(UserDTO dto) {
        User existingUser = userRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Update filed, User not exit"));

        userMapper.updateEntityFromDto(dto, existingUser);

        return userMapper.toDTO(userRepository.save(existingUser));
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("Delete filed, User not exit");
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomUserDetails login(LoginRequset data) {
        log.info("User try login: {}", data.getEmail());

        if (loginAttemptService.isBlocked(data.getEmail())) {
            log.warn(" {} you try up 15", data.getEmail());
            throw new RuntimeException("try after 15 m");
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.getEmail(), data.getPassword()));

            stopWatch.stop();
            log.info("login succsfull in {} ms", stopWatch.getTotalTimeMillis());

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            loginAttemptService.loginSucceeded(data.getEmail());

            return userDetails;

        } catch (BadCredentialsException e) {
            log.warn("error data login {}", data.getEmail());
            loginAttemptService.loginFailed(data.getEmail());
            throw new RuntimeException("email or password invalid");
        } catch (DisabledException e) {
            log.warn("account user {} unactive", data.getEmail());
            throw new RuntimeException("your account unactive");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndActive(email, true)
                .orElseThrow(() -> {
                    log.error("not found user by email: {}", email);
                    return new EntityNotFoundException("not found user or is not active");
                });
    }
}