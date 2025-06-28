package com.example.server.service;

import com.example.server.exception.BusinessException;
import com.example.server.model.data.Transaction;
import com.example.server.model.vo.TransactionSearchVO;
import com.example.server.model.vo.TransactionVO;
import com.example.server.repository.TransactionRepository;
import com.example.server.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getTransactionById_success() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        TransactionVO vo = transactionService.getTransactionById(1L, "user1");
        assertEquals(1L, vo.getId());
    }

    @Test
    void getTransactionById_notFound() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> transactionService.getTransactionById(1L, "user1"));
    }

    @Test
    void deleteTransaction_success() {
        Transaction transaction = new Transaction();
        transaction.setId(2L);
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(transaction));
        doNothing().when(transactionRepository).deleteById(2L);

        assertTrue(transactionService.deleteTransaction(2L, "user1"));
        verify(transactionRepository).deleteById(2L);
    }

    @Test
    void deleteTransaction_notFound() {
        when(transactionRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> transactionService.deleteTransaction(2L, "user1"));
    }

    @Test
    void searchTransaction_success() {
        TransactionSearchVO vo = new TransactionSearchVO();
        vo.setSortBy(Constants.ASC);
        vo.setPage(0);
        vo.setPageSize(10);

        Page<Transaction> page = new PageImpl<>(List.of());
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = transactionService.searchTransaction(vo);
        assertNull(result);
    }

    @Test
    void searchTransaction_pagination_shouldReturnCorrectPage() {
        TransactionSearchVO vo = new TransactionSearchVO();
        vo.setPage(1);
        vo.setPageSize(2);

        List<Transaction> transactions = Arrays.asList(new Transaction(), new Transaction());
        Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(1, 2), 5);

        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = transactionService.searchTransaction(vo);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getNumber());
    }

    @Test
    void searchTransaction_withTypeAndStatus_shouldFilter() {
        TransactionSearchVO vo = new TransactionSearchVO();
        vo.setType("PAYMENT");
        vo.setStatus("APPROVED");
        vo.setPage(0);
        vo.setPageSize(10);

        Page<Transaction> page = new PageImpl<>(Collections.singletonList(new Transaction()));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = transactionService.searchTransaction(vo);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void searchTransaction_withDateRange_shouldFilter() {
        TransactionSearchVO vo = new TransactionSearchVO();
        vo.setStartDate(LocalDateTime.now().minusDays(5));
        vo.setEndDate(LocalDateTime.now());
        vo.setPage(0);
        vo.setPageSize(10);

        Page<Transaction> page = new PageImpl<>(Collections.emptyList());
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = transactionService.searchTransaction(vo);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void searchTransaction_withFuzzyDescription_shouldFilter() {
        TransactionSearchVO vo = new TransactionSearchVO();
        vo.setTransactionDescription("test");
        vo.setPage(0);
        vo.setPageSize(10);

        Page<Transaction> page = new PageImpl<>(Collections.singletonList(new Transaction()));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = transactionService.searchTransaction(vo);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void searchTransaction_sortByType_shouldSort() {
        TransactionSearchVO vo = new TransactionSearchVO();
        vo.setSortBy("type");
        vo.setPage(0);
        vo.setPageSize(10);

        Page<Transaction> page = new PageImpl<>(Collections.singletonList(new Transaction()));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = transactionService.searchTransaction(vo);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void handleTransaction_approve_success() {
        TransactionVO vo = buildValidTransactionVO(3L);
        Transaction transaction = buildValidTransaction(3L, Constants.TX_STATUS_SUBMITTED);

        when(transactionRepository.findById(3L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.updateTransactionStatusById(eq(3L), eq(Constants.TX_STATUS_APPROVED), any())).thenReturn(1);

        int result = transactionService.handleTransaction(vo, Constants.TX_CONTEXT_APPROVE, "user1");
        assertEquals(1, result);
    }

    @Test
    void handleTransaction_invalidContext() {
        TransactionVO vo = buildValidTransactionVO(4L);
        assertThrows(BusinessException.class, () -> transactionService.handleTransaction(vo, "INVALID", "user1"));
    }

    @Test
    void handleTransaction_notFound() {
        TransactionVO vo = buildValidTransactionVO(5L);
        when(transactionRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> transactionService.handleTransaction(vo, Constants.TX_CONTEXT_APPROVE, "user1"));
    }

    @Test
    void handleTransaction_statusNotSubmitted() {
        TransactionVO vo = buildValidTransactionVO(6L);
        Transaction transaction = buildValidTransaction(6L, "APPROVED");
        when(transactionRepository.findById(6L)).thenReturn(Optional.of(transaction));
        assertThrows(BusinessException.class, () -> transactionService.handleTransaction(vo, Constants.TX_CONTEXT_APPROVE, "user1"));
    }

    @Test
    void createTransaction_success() {
        TransactionVO vo = buildValidTransactionVO(null);
        Transaction saved = new Transaction();
        saved.setId(7L);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionVO result = transactionService.createTransaction(vo, Constants.TX_CONTEXT_CREATE, "user1");
        assertEquals(7L, result.getId());
    }

    @Test
    void createTransaction_invalidParam() {
        TransactionVO vo = new TransactionVO();
        vo.setId(8L); // 不应有ID
        assertThrows(BusinessException.class, () -> transactionService.createTransaction(vo, Constants.TX_CONTEXT_CREATE, "user1"));
    }

    @Test
    void updateTransactionBasicInfo_success() {
        TransactionVO vo = buildValidTransactionVO(9L);
        Transaction transaction = buildValidTransaction(9L, Constants.TX_STATUS_SUBMITTED);

        when(transactionRepository.findById(9L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.updateTransactionBasicInfoById(eq(9L), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        int result = transactionService.updateTransactionBasicInfo(vo, Constants.TX_CONTEXT_UPDATE, "user1");
        assertEquals(1, result);
    }

    @Test
    void updateTransactionBasicInfo_notFound() {
        TransactionVO vo = buildValidTransactionVO(10L);
        when(transactionRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> transactionService.updateTransactionBasicInfo(vo, Constants.TX_CONTEXT_UPDATE, "user1"));
    }

    private TransactionVO buildValidTransactionVO(Long id) {
        TransactionVO vo = new TransactionVO();
        vo.setId(id);
        vo.setType("PAYMENT");
        vo.setAmount(BigDecimal.TEN);
        vo.setTransactionDate(LocalDateTime.now());
        vo.setTransactionDescription("desc");
        vo.setDebitAccount("debit");
        vo.setCreditAccount("credit");
        vo.setCurrency("USD");
        return vo;
    }

    private Transaction buildValidTransaction(Long id, String status) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setStatus(status);
        return t;
    }
}