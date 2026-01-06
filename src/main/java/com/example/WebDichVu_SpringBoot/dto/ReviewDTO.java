package com.example.WebDichVu_SpringBoot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

import com.example.WebDichVu_SpringBoot.entity.Review;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private UserReviewDTO user; // Sẽ là DTO, không phải Entity
    private Double rating;
    private String noiDung;
    private LocalDateTime createdAt;

    public ReviewDTO(Review review) {
        this.id = review.getId();
        this.rating = review.getRating();
        this.noiDung = review.getNoiDung();
        this.createdAt = review.getNgayTao();

        // Ngăn lỗi nếu user bị null (mặc dù đang là nullable=false)
        if (review.getUser() != null) {
            this.user = new UserReviewDTO(
                    review.getUser().getHoTen(),
                    review.getUser().getAvatarURL());
        } else {
            this.user = new UserReviewDTO("Khách hàng ẩn danh", null);
        }
    }
}