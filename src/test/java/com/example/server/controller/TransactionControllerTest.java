
package com.example.server.controller;

import com.example.server.model.data.Transaction;
import com.example.server.model.vo.TransactionVO;
import com.example.server.service.TransactionService;
import com.example.server.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTransaction_shouldReturnSuccess() throws Exception {
        TransactionVO input = new TransactionVO();
        input.setType("PAYMENT");
        TransactionVO output = new TransactionVO();
        output.setType("PAYMENT");
        output.setId(1L);

        when(transactionService.createTransaction(any(TransactionVO.class), eq(Constants.TX_CONTEXT_CREATE), eq("user1")))
                .thenReturn(output);

        mockMvc.perform(post("/transaction/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))
                        .param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.type").value("PAYMENT"));
    }

    @Test
    void updateTransaction_shouldReturnSuccess() throws Exception {
        TransactionVO input = new TransactionVO();
        input.setType("PAYMENT");

        when(transactionService.updateTransactionBasicInfo(any(TransactionVO.class), eq(Constants.TX_CONTEXT_UPDATE), eq("user1")))
                .thenReturn(1);

        mockMvc.perform(put("/transaction/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))
                        .param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void approveTransaction_shouldReturnSuccess() throws Exception {
        TransactionVO input = new TransactionVO();
        input.setId(3L);

        when(transactionService.handleTransaction(any(TransactionVO.class), eq(Constants.TX_CONTEXT_APPROVE), eq("user1")))
                .thenReturn(1);

        mockMvc.perform(post("/transaction/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))
                        .param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void rejectTransaction_shouldReturnSuccess() throws Exception {
        TransactionVO input = new TransactionVO();
        input.setId(4L);

        when(transactionService.handleTransaction(any(TransactionVO.class), eq(Constants.TX_CONTEXT_REJECT), eq("user1")))
                .thenReturn(1);

        mockMvc.perform(post("/transaction/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))
                        .param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void cancelTransaction_shouldReturnSuccess() throws Exception {
        TransactionVO input = new TransactionVO();
        input.setId(5L);

        when(transactionService.handleTransaction(any(TransactionVO.class), eq(Constants.TX_CONTEXT_CANCEL), eq("user1")))
                .thenReturn(1);

        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))
                        .param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void getTransactionById_shouldReturnSuccess() throws Exception {
        TransactionVO output = new TransactionVO();
        output.setId(6L);

        when(transactionService.getTransactionById(eq(6L), eq("user1"))).thenReturn(output);

        mockMvc.perform(get("/transaction/6")
                        .param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(6L));
    }

    @Test
    void deleteTransaction_shouldReturnSuccess() throws Exception {
        when(transactionService.deleteTransaction(eq(7L), eq("user1"))).thenReturn(true);

        mockMvc.perform(delete("/transaction/7")
                        .param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void searchTransaction_shouldReturnSuccess() throws Exception {
        when(transactionService.searchTransaction(any())).thenReturn(Page.empty());

        mockMvc.perform(post("/transaction/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void searchTransaction_withPagination_shouldReturnPagedResult() throws Exception {
        // 构造分页内容
        List<Transaction> transactions = new ArrayList<>();
        Transaction t1 = new Transaction();
        t1.setId(100L);
        transactions.add(t1);

        Page<Transaction> page = new org.springframework.data.domain.PageImpl<>(
                transactions,
                org.springframework.data.domain.PageRequest.of(1, 10),
                25 // 总条数
        );

        when(transactionService.searchTransaction(any())).thenReturn(page);

        mockMvc.perform(post("/transaction/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100L))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void searchTransaction_emptyPage_shouldReturnEmptyContent() throws Exception {
        Page<Transaction> emptyPage = Page.empty(org.springframework.data.domain.PageRequest.of(0, 5));
        when(transactionService.searchTransaction(any())).thenReturn(emptyPage);

        mockMvc.perform(post("/transaction/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":0,\"pageSize\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }
}