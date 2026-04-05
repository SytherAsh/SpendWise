package com.spendwisebackend.spendwisebackend.transaction.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.spendwisebackend.spendwisebackend.transaction.models.dto.TransactionDTO;
import com.spendwisebackend.spendwisebackend.transaction.services.TransactionService;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionDTO>> getTransactionsByAccount(
            @PathVariable UUID accountId,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        return new ResponseEntity<>(transactionService.createTransaction(transactionDTO), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/logic")
    public ResponseEntity<TransactionDTO> getTransactionWithLogic(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionByIdAndLogic(id));
    }
}