package com.example.WebDichVu_SpringBoot.dto;

// Dùng Lombok hoặc tạo getters/setters thủ công
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class EmployeeUpdateRequest {
    private String sdt;
    private String trangThaiLamViec;
    private Long assignedServiceId; // ID dịch vụ được gán
}