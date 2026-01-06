package com.example.WebDichVu_SpringBoot.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String hoTen;
    private String email;
    private String matKhau;
    // Không cần vaiTro vì ta mặc định là Role.KHACH
}