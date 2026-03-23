package com.sawash.SpendWise_Backend.repository;

import com.sawash.SpendWise_Backend.entity.TransactionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
}