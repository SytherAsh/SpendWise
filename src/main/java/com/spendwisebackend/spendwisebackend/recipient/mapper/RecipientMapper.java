package com.spendwisebackend.spendwisebackend.recipient.mapper;

import org.mapstruct.Mapper;

import com.spendwisebackend.spendwisebackend.recipient.models.dto.RecipientDTO;
import com.spendwisebackend.spendwisebackend.recipient.models.entities.Recipient;

@Mapper(componentModel = "spring")
public interface RecipientMapper {
    RecipientDTO toDto(Recipient recipient);
    Recipient toEntity(RecipientDTO recipientDto);
}