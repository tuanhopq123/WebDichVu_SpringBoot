package com.example.WebDichVu_SpringBoot.dto;

import lombok.Data;

import java.time.LocalDateTime;

import com.example.WebDichVu_SpringBoot.entity.Notification;

@Data
public class NotificationDTO {
    private Long id;
    private String message; // Đây là trường JS cần (n.message)
    private String link; // Đây là trường JS cần (n.link)
    private boolean isRead;
    private LocalDateTime createdAt; // Đây là trường JS cần (n.createdAt)
    private String type; // Đây là trường JS cần (n.type)

    // Constructor để chuyển đổi từ Entity sang DTO
    public NotificationDTO(Notification entity) {
        this.id = entity.getId();
        // Gộp tiêu đề và nội dung thành 1 message, nếu nội dung rỗng thì chỉ lấy tiêu
        // đề
        this.message = entity.getNoiDung() != null && !entity.getNoiDung().isEmpty()
                ? entity.getTieuDe() + ": " + entity.getNoiDung()
                : entity.getTieuDe();
        this.link = entity.getLinkLienQuan();
        this.isRead = entity.isRead();
        this.createdAt = entity.getThoiGianGui();
        this.type = entity.getType() != null ? entity.getType().name() : "MESSAGE"; // Chuyển Enum sang String
    }
}
