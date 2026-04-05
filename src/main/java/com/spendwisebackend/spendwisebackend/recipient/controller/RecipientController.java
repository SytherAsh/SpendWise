package com.spendwisebackend.spendwisebackend.recipient.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.spendwisebackend.spendwisebackend.recipient.models.dto.RecipientDTO;
import com.spendwisebackend.spendwisebackend.recipient.services.RecipientServices;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recipients")
@RequiredArgsConstructor
public class RecipientController {

    private final RecipientServices recipientServices;

    @GetMapping
    public ResponseEntity<Page<RecipientDTO>> getAllRecipients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(recipientServices.getAllRecipients(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipientDTO> getRecipientById(@PathVariable String id) {
        return ResponseEntity.ok(recipientServices.getRecipientById(id));
    }

    @PostMapping
    public ResponseEntity<RecipientDTO> createRecipient(@Valid @RequestBody RecipientDTO recipientDTO) {
        return new ResponseEntity<>(recipientServices.createRecipient(recipientDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipientDTO> updateRecipient(
            @PathVariable String id,
            @Valid @RequestBody RecipientDTO recipientDTO) {
        return ResponseEntity.ok(recipientServices.updateRecipient(id, recipientDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipient(@PathVariable String id) {
        recipientServices.deleteRecipient(id);
        return ResponseEntity.noContent().build();
    }
}
