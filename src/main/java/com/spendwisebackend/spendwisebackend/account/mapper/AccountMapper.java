package com.spendwisebackend.spendwisebackend.account.mapper;

import org.mapstruct.Mapper;

import com.spendwisebackend.spendwisebackend.account.models.dto.AccountDTO;
import com.spendwisebackend.spendwisebackend.account.models.entities.Accounts;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountDTO toDto(Accounts account);
    Accounts toEntity(AccountDTO accountDto);
}