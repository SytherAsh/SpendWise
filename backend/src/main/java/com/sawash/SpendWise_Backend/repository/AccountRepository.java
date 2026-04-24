package com.sawash.SpendWise_Backend.repository;

import com.sawash.SpendWise_Backend.entity.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findFirstByBankName(String bankName);
}