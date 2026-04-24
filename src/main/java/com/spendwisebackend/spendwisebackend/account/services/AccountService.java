package com.spendwisebackend.spendwisebackend.account.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spendwisebackend.spendwisebackend.account.models.dto.AccountDTO;

import java.util.UUID;

public interface AccountService {
    Page<AccountDTO> getAllAccounts(Pageable pageable);
    AccountDTO getAccountById(UUID id);
    AccountDTO createAccount(AccountDTO accountDTO);
    AccountDTO updateAccount(UUID id, AccountDTO accountDTO);
    void deleteAccount(UUID id);
}
