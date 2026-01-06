package com.example.WebDichVu_SpringBoot.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotNull(message = "Service ID không được để trống")
    private Long serviceId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @DecimalMin(value = "0.5", message = "Điểm đánh giá phải từ 0.5 đến 5.0")
    @DecimalMax(value = "5.0", message = "Điểm đánh giá phải từ 0.5 đến 5.0")
    private Double rating;

    @Size(max = 500, message = "Nội dung đánh giá không được vượt quá 500 ký tự") // Bỏ NotBlank, cho phép empty/null
    private String noiDung;
}