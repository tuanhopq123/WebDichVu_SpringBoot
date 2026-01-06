package com.example.WebDichVu_SpringBoot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReviewDTO {
    private String hoTen;
    private String avatarURL;
}