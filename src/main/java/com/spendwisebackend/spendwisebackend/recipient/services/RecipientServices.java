package com.spendwisebackend.spendwisebackend.recipient.services;

import org.springframework.data.domain.Page;

import com.spendwisebackend.spendwisebackend.recipient.models.dto.RecipientDTO;

public interface RecipientServices {
    public Page<RecipientDTO> getAllRecipients(int page, int size);
    public RecipientDTO getRecipientById(String recipientId);
    public RecipientDTO createRecipient(RecipientDTO recipientDTO);
    public RecipientDTO updateRecipient(String recipientId, RecipientDTO recipientDTO);
    public void deleteRecipient(String recipientId);
}
