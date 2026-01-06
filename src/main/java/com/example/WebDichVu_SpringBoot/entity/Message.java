package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "MESSAGES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người gửi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User sender;

    // Người nhận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User recipient;

    @Column(name = "noi_dung", columnDefinition = "TEXT", nullable = false)
    private String noiDung;

    @Column(name = "thoi_gian_gui", nullable = false)
    private LocalDateTime thoiGianGui = LocalDateTime.now();

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;
}