package com.example.WebDichVu_SpringBoot.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ServiceRequest {
    private String tenDichVu;
    private Double giaCoBan;
    private Integer thoiGianHoanThanh;
    private String moTa;
    private Long categoryId;
    private MultipartFile imageFile;
}