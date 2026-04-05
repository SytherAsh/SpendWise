package com.spendwisebackend.spendwisebackend.account.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spendwisebackend.spendwisebackend.account.models.entities.Accounts;

@Repository
public interface AccountRepository extends JpaRepository<Accounts, UUID> {
    List<Accounts> findByBankNameContainingIgnoreCase(String bankName);
}