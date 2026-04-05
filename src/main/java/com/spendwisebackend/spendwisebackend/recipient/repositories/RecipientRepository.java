package com.spendwisebackend.spendwisebackend.recipient.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spendwisebackend.spendwisebackend.recipient.models.entities.Recipient;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, UUID> {
    Optional<Recipient> findByUpiId(UUID upiId);
    Page<Recipient> findByUserId(UUID userId, Pageable pageable);
}