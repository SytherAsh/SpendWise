package com.spendwisebackend.spendwisebackend.recipient.services.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spendwisebackend.spendwisebackend.config.exceptions.ResourceNotFoundException;
import com.spendwisebackend.spendwisebackend.recipient.mapper.RecipientMapper;
import com.spendwisebackend.spendwisebackend.recipient.models.dto.RecipientDTO;
import com.spendwisebackend.spendwisebackend.recipient.models.entities.Recipient;
import com.spendwisebackend.spendwisebackend.recipient.repositories.RecipientRepository;
import com.spendwisebackend.spendwisebackend.recipient.services.RecipientServices;
import com.spendwisebackend.spendwisebackend.config.Security.SecurityUtils;
import com.spendwisebackend.spendwisebackend.audit.annotations.Auditable;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipientServicesImpl implements RecipientServices {
    private final RecipientRepository recipientRepository;
    private final RecipientMapper recipientMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<RecipientDTO> getAllRecipients(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        UUID userId = SecurityUtils.getCurrentUserId();
        return recipientRepository.findByUserId(userId, pageable).map(recipientMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipientDTO getRecipientById(String recipientId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Recipient recipient = recipientRepository.findById(UUID.fromString(recipientId))
                .filter(r -> r.getUserId().equals(userId.toString()))
                .orElseThrow(() -> new ResourceNotFoundException("المستلم غير موجود أو لا تملك صلاحية الوصول إليه"));
        return recipientMapper.toDto(recipient);
    }

    @Override
    @Auditable(action = "CREATE_RECIPIENT")
    @Transactional
    public RecipientDTO createRecipient(RecipientDTO recipientDTO) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Recipient recipient = recipientMapper.toEntity(recipientDTO);
        recipient.setUserId(userId.toString());
        recipient.setId(null); // Ensure creation
        return recipientMapper.toDto(recipientRepository.save(recipient));
    }

    @Override
    @Auditable(action = "UPDATE_RECIPIENT")
    @Transactional
    public RecipientDTO updateRecipient(String recipientId, RecipientDTO recipientDTO) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Recipient existing = recipientRepository.findById(UUID.fromString(recipientId))
                .filter(r -> r.getUserId().equals(userId.toString()))
                .orElseThrow(() -> new ResourceNotFoundException("لا يمكن تحديث مستلم غير موجود"));
        
        existing.setName(recipientDTO.getName());
        existing.setUpiId(recipientDTO.getUpiId());
        existing.setBankName(recipientDTO.getBankName());
        
        return recipientMapper.toDto(recipientRepository.save(existing));
    }

    @Override
    @Auditable(action = "DELETE_RECIPIENT")
    @Transactional
    public void deleteRecipient(String recipientId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Recipient recipient = recipientRepository.findById(UUID.fromString(recipientId))
                .filter(r -> r.getUserId().equals(userId.toString()))
                .orElseThrow(() -> new ResourceNotFoundException("لا يمكن حذف مستلم غير موجود"));
        recipientRepository.delete(recipient);
    }
}
