package com.sawash.SpendWise_Backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;

    @Column(name = "transaction_reference", nullable = false)
    private String transactionReference;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "debit")
    private BigDecimal debit;

    @Column(name = "credit")
    private BigDecimal credit;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "transaction_mode")
    private String transactionMode;

    @Column(name = "dr_cr_indicator")
    private String drCrIndicator;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}