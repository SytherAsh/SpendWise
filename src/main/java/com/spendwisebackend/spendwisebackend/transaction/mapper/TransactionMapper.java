package com.spendwisebackend.spendwisebackend.transaction.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.spendwisebackend.spendwisebackend.transaction.models.dto.TransactionDTO;
import com.spendwisebackend.spendwisebackend.transaction.models.entities.Transaction;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "recipient.id", target = "recipientId")
    TransactionDTO toDto(Transaction transaction);

    @Mapping(source = "accountId", target = "account.id")
    @Mapping(source = "recipientId", target = "recipient.id")
    Transaction toEntity(TransactionDTO transactionDto);
}