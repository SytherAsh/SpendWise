package com.spendwisebackend.spendwisebackend.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.spendwisebackend.spendwisebackend.user.CustomUserDetails;
import com.spendwisebackend.spendwisebackend.user.repositories.UserRepository;
import com.spendwisebackend.spendwisebackend.user.repositories.Projection.UserLoginProjection;

@Service
public class UserDetailsDataService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserLoginProjection userProjection = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new CustomUserDetails(
                userProjection.getId(),
                userProjection.getEmail(),
                userProjection.getPassword(),
                userProjection.getActive(),
                userProjection.getRole(),
                userProjection.getName(),
                userProjection.getEmail());
    }
}