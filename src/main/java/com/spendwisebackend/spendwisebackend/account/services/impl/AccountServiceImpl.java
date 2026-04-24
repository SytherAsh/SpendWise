package com.spendwisebackend.spendwisebackend.account.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spendwisebackend.spendwisebackend.account.mapper.AccountMapper;
import com.spendwisebackend.spendwisebackend.account.models.dto.AccountDTO;
import com.spendwisebackend.spendwisebackend.account.models.entities.Accounts;
import com.spendwisebackend.spendwisebackend.account.repositories.AccountRepository;
import com.spendwisebackend.spendwisebackend.account.services.AccountService;
import com.spendwisebackend.spendwisebackend.config.exceptions.ResourceNotFoundException;
import com.spendwisebackend.spendwisebackend.audit.annotations.Auditable;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AccountDTO> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(accountMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(UUID id) {
        return accountRepository.findById(id)
                .map(accountMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("الحساب غير موجود!"));
    }

    @Override
    @Auditable(action = "CREATE_ACCOUNT")
    @Transactional
    public AccountDTO createAccount(AccountDTO accountDTO) {
        Accounts account = accountMapper.toEntity(accountDTO);
        account.setId(null);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Override
    @Auditable(action = "UPDATE_ACCOUNT")
    @Transactional
    public AccountDTO updateAccount(UUID id, AccountDTO accountDTO) {
        Accounts existing = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("لا يمكن تحديث حساب غير موجود"));
        
        existing.setBankName(accountDTO.getBankName());
        existing.setAccountType(accountDTO.getAccountType());
        
        return accountMapper.toDto(accountRepository.save(existing));
    }

    @Override
    @Auditable(action = "DELETE_ACCOUNT")
    @Transactional
    public void deleteAccount(UUID id) {
        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("لا يمكن حذف حساب غير موجود");
        }
        accountRepository.deleteById(id);
    }
}
