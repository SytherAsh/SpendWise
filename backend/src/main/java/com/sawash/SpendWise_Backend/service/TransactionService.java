package com.sawash.SpendWise_Backend.service;

import com.sawash.SpendWise_Backend.client.FastApiClient;
import com.sawash.SpendWise_Backend.dto.*;
import com.sawash.SpendWise_Backend.entity.*;
import com.sawash.SpendWise_Backend.exception.ResourceNotFoundException;
import com.sawash.SpendWise_Backend.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final RecipientRepository recipientRepository;
    private final FastApiClient fastApiClient;

    @Transactional
    public TransactionResponse create(TransactionCreateRequest req) {
        BigDecimal debit = nz(req.getDebit());
        BigDecimal credit = nz(req.getCredit());
        BigDecimal amount = normalizeAmount(req.getAmount(), debit, credit);

        Account account = accountRepository.findFirstByBankName(s(req.getBank()))
                .orElseGet(() -> accountRepository.save(Account.builder().bankName(s(req.getBank())).accountType("SAVINGS").build()));

        Recipient recipient = (req.getUpiId() != null && !req.getUpiId().isBlank())
                ? recipientRepository.findFirstByUpiId(req.getUpiId()).orElseGet(() -> recipientRepository.save(
                Recipient.builder().name(s(req.getRecipientName())).upiId(req.getUpiId()).bankName(s(req.getBank())).build()))
                : recipientRepository.save(Recipient.builder().name(s(req.getRecipientName())).upiId(null).bankName(s(req.getBank())).build());

        String description = (req.getDescription() == null || req.getDescription().isBlank()) ? req.getNote() : req.getDescription();
        String direction = debit.compareTo(BigDecimal.ZERO) > 0 ? "DR" : "CR";

        String category = "UNCATEGORIZED";
        BigDecimal categoryConfidence = BigDecimal.ZERO;
        String modelVersion = "fallback-v1";
        String mlStatus = "FAILED";

        try {
            FastApiCategorizeResponse mlResponse = fastApiClient.categorize(
                    FastApiCategorizeRequest.builder()
                            .description(description)
                            .amount(amount)
                            .transactionMode(req.getTransactionMode())
                            .drCrIndicator(direction)
                            .build()
            );

            if (mlResponse != null && mlResponse.getCategory() != null && !mlResponse.getCategory().isBlank()) {
                category = mlResponse.getCategory();
                categoryConfidence = mlResponse.getConfidence() == null ? BigDecimal.ZERO : mlResponse.getConfidence();
                modelVersion = mlResponse.getModelVersion() == null ? "dummy-v1" : mlResponse.getModelVersion();
                mlStatus = "SUCCESS";
            }
        } catch (Exception ex) {
            log.warn("Categorization failed for reference {}: {}", req.getTransactionReference(), ex.getMessage());
        }

        TransactionEntity tx = TransactionEntity.builder()
                .account(account)
                .recipient(recipient)
                .transactionReference(req.getTransactionReference())
                .transactionDate(req.getTransactionDate() == null || req.getTransactionDate().isBlank() ? null : LocalDate.parse(req.getTransactionDate()))
                .amount(amount)
                .debit(debit)
                .credit(credit)
                .balance(req.getBalance())
                .transactionMode(req.getTransactionMode())
                .drCrIndicator(direction)
                .note(req.getNote())
                .description(description)
                .category(category)
                .categoryConfidence(categoryConfidence)
                .mlModelVersion(modelVersion)
                .mlStatus(mlStatus)
                .build();

        return map(transactionRepository.save(tx));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id) {
        return map(transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findAll(pageable).map(this::map);
    }

    @Transactional(readOnly = true)
    public TransactionLogicResponse logic(UUID id) {
        TransactionEntity tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));

        BigDecimal debit = nz(tx.getDebit());
        BigDecimal credit = nz(tx.getCredit());
        BigDecimal effective = debit.compareTo(BigDecimal.ZERO) > 0 ? debit : credit;
        String direction = debit.compareTo(BigDecimal.ZERO) > 0 ? "DEBIT" : "CREDIT";

        String sizeBucket = effective.compareTo(new BigDecimal("100000")) >= 0 ? "HIGH"
                : effective.compareTo(new BigDecimal("10000")) >= 0 ? "MEDIUM" : "LOW";

        return TransactionLogicResponse.builder()
                .transactionId(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .direction(direction)
                .effectiveAmount(effective)
                .sizeBucket(sizeBucket)
                .transactionMode(tx.getTransactionMode() == null ? "UNKNOWN" : tx.getTransactionMode())
                .note(tx.getNote())
                .build();
    }

    @Transactional
    public void delete(UUID id) {
        TransactionEntity tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        transactionRepository.delete(tx);
    }

    private BigDecimal normalizeAmount(BigDecimal amount, BigDecimal debit, BigDecimal credit) {
        if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Only one of debit or credit can be positive");
        }
        if (debit.compareTo(BigDecimal.ZERO) > 0) return debit.negate();
        if (credit.compareTo(BigDecimal.ZERO) > 0) return credit;
        if (amount == null) throw new IllegalArgumentException("amount or debit/credit is required");
        return amount;
    }

    private TransactionResponse map(TransactionEntity tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .accountId(tx.getAccount().getId())
                .recipientId(tx.getRecipient().getId())
                .transactionReference(tx.getTransactionReference())
                .transactionDate(tx.getTransactionDate())
                .amount(tx.getAmount())
                .debit(tx.getDebit())
                .credit(tx.getCredit())
                .balance(tx.getBalance())
                .transactionMode(tx.getTransactionMode())
                .drCrIndicator(tx.getDrCrIndicator())
                .note(tx.getNote())
                .description(tx.getDescription())
                .category(tx.getCategory())
                .categoryConfidence(tx.getCategoryConfidence())
                .mlModelVersion(tx.getMlModelVersion())
                .mlStatus(tx.getMlStatus())
                .build();
    }

    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private String s(String value) {
        if (value == null || value.isBlank()) return "UNKNOWN_BANK";
        return value;
    }
}