package com.example.server.model.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String type;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "transaction_description", length = 255)
    private String transactionDescription;

    @Column(name = "debit_account", length = 50)
    private String debitAccount;

    @Column(name = "credit_account", length = 50)
    private String creditAccount;

    @Column(length = 50)
    private String status;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(length = 10)
    private String currency;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}