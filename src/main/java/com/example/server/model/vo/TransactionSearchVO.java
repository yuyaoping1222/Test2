package com.example.server.model.vo;

import com.example.server.util.Constants;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionSearchVO {
    private Long id;
    private String type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String transactionDescription;
    private String status;
    private String submittedBy;
    private String approvedBy;

    private int page = 0;
    private int pageSize = 50;
    private String sortBy = "id";
    private String sortDirection = Constants.DESC;
}
