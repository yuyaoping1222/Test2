package com.example.server.controller;

import com.example.server.model.data.Transaction;
import com.example.server.model.vo.TransactionSearchVO;
import com.example.server.model.vo.TransactionVO;
import com.example.server.service.TransactionService;
import com.example.server.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import com.example.server.model.ApiResponse;

@Slf4j
@RestController()
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
        log.info("TransactionController initialized with TransactionService");
    }

    // create a transaction
    @PostMapping("/create")
    public ApiResponse<TransactionVO> createTransaction(@RequestBody TransactionVO transaction,
                                                      @RequestParam String userId) {
        return ApiResponse.success(transactionService.createTransaction(transaction, Constants.TX_CONTEXT_CREATE, userId));
    }

    @PutMapping("/update")
    public ApiResponse<Integer> updateTransaction(@RequestBody TransactionVO transaction,
                                                      @RequestParam String userId) {
        return ApiResponse.success(transactionService.updateTransactionBasicInfo(transaction, Constants.TX_CONTEXT_UPDATE, userId));
    }

    @PostMapping("/approve")
    public ApiResponse<Integer> approveTransaction(@RequestBody TransactionVO transaction,
                                                       @RequestParam String userId) {
        Integer approved = transactionService.handleTransaction(transaction, Constants.TX_CONTEXT_APPROVE , userId);
        return ApiResponse.success(approved);
    }

    @PostMapping("/reject")
    public ApiResponse<Integer> rejectTransaction(@RequestBody TransactionVO transaction,
                                                         @RequestParam String userId) {
        Integer approved = transactionService.handleTransaction(transaction, Constants.TX_CONTEXT_REJECT , userId);
        return ApiResponse.success(approved);
    }

    @PostMapping("/cancel")
    public ApiResponse<Integer> cancelTransaction(@RequestBody TransactionVO transaction,
                                                         @RequestParam String userId) {
        Integer approved = transactionService.handleTransaction(transaction, Constants.TX_CONTEXT_CANCEL , userId);
        return ApiResponse.success(approved);
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionVO> getTransactionById(@PathVariable Long id,
                                                       @RequestParam String userId) {
        return ApiResponse.success(transactionService.getTransactionById(id, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteTransaction(@PathVariable Long id,
                                                  @RequestParam String userId) {
        return ApiResponse.success(transactionService.deleteTransaction(id, userId));
    }

    @PostMapping("/search")
    public Page<Transaction> searchTransaction(@RequestBody TransactionSearchVO searchVO) {
        return transactionService.searchTransaction(searchVO);
    }
}
