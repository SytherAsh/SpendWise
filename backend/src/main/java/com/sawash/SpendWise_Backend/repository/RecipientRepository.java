package com.sawash.SpendWise_Backend.repository;

import com.sawash.SpendWise_Backend.entity.Recipient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientRepository extends JpaRepository<Recipient, UUID> {
    Optional<Recipient> findFirstByUpiId(String upiId);
}
