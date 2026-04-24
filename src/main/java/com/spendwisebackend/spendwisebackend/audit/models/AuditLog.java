package com.spendwisebackend.spendwisebackend.audit.models;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String userEmail;
    private String action;
    private String resource;
    private String ipAddress;
    
    @Column(columnDefinition = "TEXT")
    private String details;

    private LocalDateTime timestamp;
}
