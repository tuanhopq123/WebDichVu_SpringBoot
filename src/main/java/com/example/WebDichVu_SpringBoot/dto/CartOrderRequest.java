package com.example.WebDichVu_SpringBoot.dto;

import lombok.Data;
import java.util.List;

@Data
public class CartOrderRequest {
    private List<Long> serviceIds; // Danh sách ID các dịch vụ được chọn
    private String thoiGianDat; // "2023-10-25T14:30"
    private String diaChiDichVu;
    private String sdt;
    private String notes;
    private String phuongThucThanhToan; // "TIEN_MAT" hoặc "CHUYEN_KHOAN"
}