package com.example.WebDichVu_SpringBoot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CassoTransaction {

    private Long id;

    // Nội dung chuyển khoản (Dùng để lấy Order ID)
    private String description;

    // Số tiền đã chuyển
    private BigDecimal amount;

    private String reference;

    @JsonProperty("transactionDateTime")
    private String transactionDateTime;

    @JsonProperty("bankName")
    private String bankName;

}