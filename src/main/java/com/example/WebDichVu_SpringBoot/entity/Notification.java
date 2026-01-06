package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người nhận thông báo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User recipient;

    @Column(name = "tieu_de", nullable = false)
    private String tieuDe;

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "link_lien_quan")
    private String linkLienQuan; // Đường dẫn khi người dùng nhấp vào thông báo

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // Đã đọc hay chưa

    @Column(name = "thoi_gian_gui", nullable = false)
    private LocalDateTime thoiGianGui = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private Type type; // Loại thông báo (ORDER, REVIEW, PROMOTION)

    public enum Type {
        ORDER, MESSAGE, PROMOTION
    }
}