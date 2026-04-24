package com.spendwisebackend.spendwisebackend.user.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.spendwisebackend.spendwisebackend.user.models.entities.User;
import com.spendwisebackend.spendwisebackend.user.repositories.Projection.UserLoginProjection;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<UserLoginProjection> findByEmail(String email);

    Optional<User> findByEmailAndActive(String email, boolean active);
}
