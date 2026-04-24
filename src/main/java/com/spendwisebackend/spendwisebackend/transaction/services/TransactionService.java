package com.spendwisebackend.spendwisebackend.transaction.services;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spendwisebackend.spendwisebackend.transaction.models.dto.TransactionDTO;

import java.util.UUID;

public interface TransactionService {
    TransactionDTO createTransaction(TransactionDTO transactionDTO);
    TransactionDTO getTransactionById(UUID id);
    Page<TransactionDTO> getTransactionsByAccountId(UUID accountId, Pageable pageable);
    TransactionDTO getTransactionByIdAndLogic(UUID id);
    
}