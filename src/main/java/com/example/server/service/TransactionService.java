package com.example.server.service;

import com.example.server.exception.BusinessException;
import com.example.server.model.ExecutionCode;
import com.example.server.model.vo.TransactionSearchVO;
import com.example.server.model.data.Transaction;
import com.example.server.model.vo.TransactionVO;
import com.example.server.repository.TransactionRepository;
import com.example.server.util.Constants;
import jakarta.persistence.criteria.*;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionService {

    // This service will handle transaction-related operations
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
        log.info("TransactionService initialized with TransactionRepository");
    }

    @Transactional
    @Cacheable(value = "transaction", key = "#transactionId")
    public TransactionVO getTransactionById(Long transactionId, String userId) {
        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new BusinessException(ExecutionCode.NOT_FOUND));

            TransactionVO transactionVO = new TransactionVO();
            BeanUtils.copyProperties(transaction, transactionVO);
            log.info("Transaction detail of " + transactionId + " is retrieved successfully for user: " + userId);
            return transactionVO;
        } catch (Exception e) {
            log.error("Failed to retrieve transaction with id: {}", transactionId, e);
            throw e;
        }
    }

    @Transactional
    public Boolean deleteTransaction(Long transactionId, String userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ExecutionCode.NOT_FOUND));

        transactionRepository.deleteById(transaction.getId());
        log.info("Transaction " + transactionId + "gets deleted by user: " + userId);

        return true;
    }

    //to-do search transaction with criteria and pagination
    @Transactional
    public Page<Transaction> searchTransaction(TransactionSearchVO transactionSearchVO) {
        // build search criteria based on the TransactionSearchVO
        Specification<Transaction> specification = buildTransactionSearchSpecification(transactionSearchVO);

        String sortBy = transactionSearchVO.getSortBy();
        Sort.Order order = Sort.Order.desc("id"); // Default sort order

        if (sortBy == null || sortBy.isEmpty() || "id".equalsIgnoreCase(sortBy)) {
            order = new Sort.Order(Sort.Direction.DESC, "id");
        } else if (isValidColumn(sortBy)) {
            order = new Sort.Order(Sort.Direction.DESC, sortBy);
        }
        Pageable transactionPage = PageRequest.of(
                transactionSearchVO.getPage(),
                transactionSearchVO.getPageSize(),
                Sort.by(order)
        );
        return transactionRepository.findAll(specification, transactionPage);
    }

    @Transactional
    @CacheEvict(value = "transaction", key = "#transactionVO.id")
    public Integer handleTransaction(TransactionVO transactionVO, String context, String userId) {
        // Validate the transaction before adding
        ExecutionCode validation = validateTransaction(transactionVO, context, userId);
        if (ExecutionCode.SUCCESS.getCode() != validation.getCode()) {
            log.error("Transaction validation failed for {} operation by user {}", context, userId);
            throw new BusinessException(validation);
        }

        String status = switch (context) {
            case Constants.TX_CONTEXT_APPROVE -> Constants.TX_STATUS_APPROVED;
            case Constants.TX_CONTEXT_CANCEL -> Constants.TX_STATUS_CANCELLED;
            case Constants.TX_CONTEXT_REJECT -> Constants.TX_STATUS_REJECTED;
            default -> {
                log.error("Invalid context for transaction handling: {}", context);
                throw new BusinessException(ExecutionCode.INVALID_PARAMETER);
            }
        };

        return transactionRepository.updateTransactionStatusById(transactionVO.getId(), status, LocalDateTime.now());
    }

    @Transactional
    public TransactionVO createTransaction(TransactionVO transactionVO, String context, String userId) {
        ExecutionCode validation = validateTransaction(transactionVO, context, userId);
        if (ExecutionCode.SUCCESS.getCode() != validation.getCode()) {
            log.error("Transaction validation failed for {} operation by user {}", context, userId);
            throw new BusinessException(validation);
        }

        Transaction toCreate = new Transaction();
        BeanUtils.copyProperties(transactionVO, toCreate);

        toCreate.setLastUpdated(LocalDateTime.now());
        toCreate.setSubmittedBy(userId);
        toCreate.setSubmittedAt(LocalDateTime.now());
        toCreate.setStatus(Constants.TX_STATUS_SUBMITTED);

        Transaction newTransaction = transactionRepository.save(toCreate);

        TransactionVO created = new TransactionVO();
        BeanUtils.copyProperties(newTransaction, created);

        log.info("Transaction updated successfully by user: {}", userId);
        return created;
    }

    @Transactional
    @CacheEvict(value = "transaction", key = "#transactionVO.id")
    public Integer updateTransactionBasicInfo(TransactionVO transactionVO, String context, String userId) {
        ExecutionCode validation = validateTransaction(transactionVO, context, userId);
        if (ExecutionCode.SUCCESS.getCode() != validation.getCode()) {
            log.error("Transaction validation failed for {} operation by user {}", context, userId);
            throw new BusinessException(validation);
        }

        int updateNum = transactionRepository.updateTransactionBasicInfoById(transactionVO.getId(),
                transactionVO.getType(), transactionVO.getAmount(), transactionVO.getTransactionDate(),
                transactionVO.getTransactionDescription(), transactionVO.getDebitAccount(),
                transactionVO.getCreditAccount(), transactionVO.getCurrency(), LocalDateTime.now());

        log.info("Transaction updated successfully by user: {}", userId);
        return updateNum;
    }

    private ExecutionCode validateTransaction(TransactionVO transaction, String context, String userId) {
        if (context == null || context.isEmpty()) {
            log.error("Operation context is null or empty");
            return ExecutionCode.INVALID_PARAMETER;
        }

        if (Constants.TX_CONTEXT_CREATE.equals(context)) {
            ExecutionCode validator = validateTransactionProperties(transaction, userId);
            if (ExecutionCode.SUCCESS.getCode() != validator.getCode()) {
                log.error("Transaction properties validation failed: {}", validator.getMessage());
                return validator;
            }
            if (transaction.getId() != null) {
                log.error("Transaction ID should not be set for creation");
                return ExecutionCode.INVALID_PARAMETER;
            }
            return ExecutionCode.SUCCESS;
        }

        // 其它 context 统一校验
        if (transaction.getId() == null) {
            log.error("Transaction ID is required for {}", context);
            return ExecutionCode.INVALID_PARAMETER;
        }
        Transaction currentTransaction = transactionRepository.findById(transaction.getId()).orElse(null);
        if (currentTransaction == null) {
            log.error("Transaction with ID {} does not exist", transaction.getId());
            return ExecutionCode.NOT_FOUND;
        }

        if (Constants.TX_CONTEXT_UPDATE.equals(context)) {
            ExecutionCode validator = validateTransactionProperties(transaction, userId);
            if (ExecutionCode.SUCCESS.getCode() != validator.getCode()) {
                log.error("Transaction properties validation failed: {}", validator.getMessage());
                return validator;
            }
            if (!Constants.TX_STATUS_SUBMITTED.equals(currentTransaction.getStatus())
                    && !Constants.TX_STATUS_REJECTED.equals(currentTransaction.getStatus())) {
                log.error("Transaction only SUBMITTED OR REJECTED status can {}", context);
                return ExecutionCode.BUSINESS_ERROR;
            }
            return ExecutionCode.SUCCESS;
        }

        if (Constants.TX_CONTEXT_APPROVE.equals(context)
                || Constants.TX_CONTEXT_REJECT.equals(context)
                || Constants.TX_CONTEXT_CANCEL.equals(context)) {
            if (!Constants.TX_STATUS_SUBMITTED.equals(currentTransaction.getStatus())) {
                log.error("Transaction only SUBMITTED status can {}", context);
                return ExecutionCode.BUSINESS_ERROR;
            }
            return ExecutionCode.SUCCESS;
        }

        log.error("Unknown operation context: {}", context);
        return ExecutionCode.BUSINESS_ERROR;
    }

    private ExecutionCode validateTransactionProperties(TransactionVO transaction, String userId) {
        if (transaction == null || userId == null || userId.isEmpty()) {
            return ExecutionCode.INVALID_PARAMETER;
        }
        if (transaction.getAmount() == null) {
            return ExecutionCode.INVALID_PARAMETER.withProperty("amount");
        }
        if (transaction.getTransactionDate() == null) {
            return ExecutionCode.INVALID_PARAMETER.withProperty("transactionDate");
        }
        String desc = transaction.getTransactionDescription();
        if (desc == null || desc.isEmpty() || desc.trim().isEmpty() || desc.length() > 150) {
            return ExecutionCode.INVALID_PARAMETER.withProperty("transactionDescription");
        }
        if (transaction.getDebitAccount() == null || transaction.getDebitAccount().isEmpty()) {
            return ExecutionCode.INVALID_PARAMETER.withProperty("debitAccount");
        }
        if (transaction.getCreditAccount() == null || transaction.getCreditAccount().isEmpty()) {
            return ExecutionCode.INVALID_PARAMETER.withProperty("creditAccount");
        }
        if (transaction.getCurrency() == null || transaction.getCurrency().isEmpty()) {
            return ExecutionCode.INVALID_PARAMETER.withProperty("currency");
        }
        return ExecutionCode.SUCCESS;
    }

    private Specification<Transaction> buildTransactionSearchSpecification(TransactionSearchVO vo) {
        return (Root<Transaction> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (vo.getId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("id"), vo.getId()));
            }
            if (vo.getType() != null && !vo.getType().isEmpty()) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), vo.getType()));
            }
            if (vo.getStatus() != null && !vo.getStatus().isEmpty()) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), vo.getStatus()));
            }
            if (vo.getStartDate() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("transactionDate"), vo.getStartDate()));
            }
            if (vo.getEndDate() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("transactionDate"), vo.getEndDate()));
            }
            if (vo.getTransactionDescription() != null && !vo.getTransactionDescription().isEmpty()) {
                predicate = cb.and(predicate, cb.like(root.get("transactionDescription"), "%" + vo.getTransactionDescription() + "%"));
            }
            if (vo.getSubmittedBy() != null && !vo.getSubmittedBy().isEmpty()) {
                predicate = cb.and(predicate, cb.equal(root.get("submittedBy"), vo.getSubmittedBy()));
            }
            if (vo.getApprovedBy() != null && !vo.getApprovedBy().isEmpty()) {
                predicate = cb.and(predicate, cb.equal(root.get("approvedBy"), vo.getApprovedBy()));
            }
            return predicate;
        };
    }

    private boolean isValidColumn(String column) {
        // Check if the column is a valid field in the Transaction entity
        try {
            Transaction.class.getDeclaredField(column);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

}
