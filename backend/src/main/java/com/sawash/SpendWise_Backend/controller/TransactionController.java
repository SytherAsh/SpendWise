package com.sawash.SpendWise_Backend.controller;

import com.sawash.SpendWise_Backend.dto.TransactionCreateRequest;
import com.sawash.SpendWise_Backend.dto.TransactionLogicResponse;
import com.sawash.SpendWise_Backend.dto.TransactionResponse;
import com.sawash.SpendWise_Backend.service.TransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public TransactionResponse create(@Valid @RequestBody TransactionCreateRequest request) {
        return transactionService.create(request);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) {
        return transactionService.get(id);
    }

    @GetMapping
    public Page<TransactionResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return transactionService.list(page, size);
    }

    @GetMapping("/{id}/logic")
    public TransactionLogicResponse logic(@PathVariable UUID id) {
        return transactionService.logic(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}