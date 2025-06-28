package com.example.server.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionVO {

    private Long id;
    //transactinBasicInfo
    private String type;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String transactionDescription;
    private String debitAccount;
    private String creditAccount;
    private String currency;
    //transactionOrderInfo
    private String status;
    private LocalDateTime lastUpdated;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
}
