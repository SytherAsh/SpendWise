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
import com.spendwisebackend.spendwisebackend.config.exceptions.ResourceNotFoundException;
import com.spendwisebackend.spendwisebackend.config.exceptions.BadRequestException;
import com.spendwisebackend.spendwisebackend.audit.annotations.Auditable;

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
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userMapper.toDTOList(userPage.getContent());
    }

    @Override
    @Auditable(action = "USER_REGISTER")
    @Transactional
    public UserDTO saveUser(UserDTO dto) {
        User user = userMapper.toEntity(dto);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setId(null);
        user.setActive(true);
        return userMapper.toDTO(userRepository.save(user));
    }

    @Override
    @Auditable(action = "USER_UPDATE")
    @Transactional
    public UserDTO updateUser(UserDTO dto) {
        User existingUser = userRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("فشل التحديث، المستخدم غير موجود"));

        userMapper.updateEntityFromDto(dto, existingUser);
        return userMapper.toDTO(userRepository.save(existingUser));
    }

    @Override
    @Auditable(action = "USER_DELETE")
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("فشل الحذف، المستخدم غير موجود");
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomUserDetails login(LoginRequset data) {
        log.info("Try login: {}", data.getEmail());

        if (loginAttemptService.isBlocked(data.getEmail())) {
            throw new BadRequestException("تم حظر البريد الإلكتروني مؤقتاً بسبب محاولات فاشلة متكررة، يرجى المحاولة بعد 15 دقيقة");
        }

        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.getEmail(), data.getPassword()));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            loginAttemptService.loginSucceeded(data.getEmail());

            return userDetails;

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(data.getEmail());
            throw new BadRequestException("البريد الإلكتروني أو كلمة المرور غير صحيحة");
        } catch (DisabledException e) {
            throw new BadRequestException("هذا الحساب غير نشط حالياً");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndActive(email, true)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود أو غير نشط"));
    }
}