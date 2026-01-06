package com.example.WebDichVu_SpringBoot.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data // Annotation của Lombok để tự tạo getter/setter
public class OrderRequest {
    private Long userId;
    private Long serviceId;
    private String thoiGianDat;
    private String diaChiDichVu;
    private BigDecimal tongTien;
    private String phuongThucThanhToan;
    private String trangThai;
    private String notes;
    private String sdt;
    private String paymentStatus;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}