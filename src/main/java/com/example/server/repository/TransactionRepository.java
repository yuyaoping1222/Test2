package com.example.server.repository;

import com.example.server.model.data.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    @Modifying
    @Transactional
    @Query("update Transaction t set t.type = :type, t.amount = :amount, t.transactionDate = :transactionDate, " +
            "t.transactionDescription = :transactionDescription, t.debitAccount = :debitAccount, " +
            "t.creditAccount = :creditAccount, t.currency = :currency, t.lastUpdated = :lastUpdated, t.status='SUBMITTED' where t.id = :id")
    int updateTransactionBasicInfoById(Long id, String type, BigDecimal amount, LocalDateTime transactionDate,
                                       String transactionDescription, String debitAccount,
                                       String creditAccount, String currency, LocalDateTime lastUpdated);

    @Modifying
    @Transactional
    @Query("update Transaction t set t.status = :status, t.lastUpdated = :lastUpdated where t.id = :id")
    int updateTransactionStatusById(Long id, String status, LocalDateTime lastUpdated);
}
