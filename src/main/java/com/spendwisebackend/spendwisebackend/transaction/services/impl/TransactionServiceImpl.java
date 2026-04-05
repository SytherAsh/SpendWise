package com.spendwisebackend.spendwisebackend.transaction.services.impl;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import com.spendwisebackend.spendwisebackend.config.exceptions.ResourceNotFoundException;
import com.spendwisebackend.spendwisebackend.audit.annotations.Auditable;
import com.spendwisebackend.spendwisebackend.account.models.entities.Accounts;
import com.spendwisebackend.spendwisebackend.account.repositories.AccountRepository;
import com.spendwisebackend.spendwisebackend.recipient.models.entities.Recipient;
import com.spendwisebackend.spendwisebackend.recipient.repositories.RecipientRepository;
import com.spendwisebackend.spendwisebackend.transaction.mapper.TransactionMapper;
import com.spendwisebackend.spendwisebackend.transaction.models.dto.TransactionDTO;
import com.spendwisebackend.spendwisebackend.transaction.models.entities.Transaction;
import com.spendwisebackend.spendwisebackend.transaction.repositories.TransactionRepository;
import com.spendwisebackend.spendwisebackend.transaction.services.TransactionService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final RecipientRepository recipientRepository;
    private final TransactionMapper transactionMapper;
    private final WebClient fastApiWebClient;

    @Override
    @Auditable(action = "CREATE_TRANSACTION")
    @Transactional
    public TransactionDTO createTransaction(TransactionDTO dto) {
        Accounts account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("الحساب غير موجود!"));

        Recipient recipient = recipientRepository.findById(dto.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("المستلم غير موجود!"));

        Transaction transaction = transactionMapper.toEntity(dto);

        transaction.setAccount(account);
        transaction.setRecipient(recipient);

        Transaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toDto(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("المعاملة غير موجودة!"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionsByAccountId(UUID accountId, Pageable pageable) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(transactionMapper::toDto);
    }

    @Override
    @Transactional
    public TransactionDTO getTransactionByIdAndLogic(UUID id) {
        return fastApiWebClient.get()
                .uri("/transactions/{id}/logic", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> Mono.error(new ResourceNotFoundException("خطأ في خدمة FastAPI الخارجية")))
                .bodyToMono(TransactionDTO.class)
                .block();
    }
}