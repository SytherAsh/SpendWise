package com.spendwisebackend.spendwisebackend.transaction.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

import com.spendwisebackend.spendwisebackend.recipient.models.entities.Recipient;


import com.spendwisebackend.spendwisebackend.account.models.entities.Accounts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transactions_account_id", columnList = "account_id"),
    @Index(name = "idx_transactions_recipient_id", columnList = "recipient_id"),
    @Index(name = "idx_transactions_created_at", columnList = "created_at DESC")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Accounts account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;

    @Column(name = "transaction_reference", nullable = false)
    private String transactionReference;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(precision = 14, scale = 2)
    private BigDecimal debit;

    @Column(precision = 14, scale = 2)
    private BigDecimal credit;

    @Column(precision = 14, scale = 2)
    private BigDecimal balance;

    @Column(name = "transaction_mode")
    private String transactionMode;

    @Column(name = "dr_cr_indicator")
    private String drCrIndicator;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}