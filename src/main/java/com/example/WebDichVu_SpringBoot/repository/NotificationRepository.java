package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.WebDichVu_SpringBoot.entity.Notification;

import java.util.List; // Đảm bảo có import này

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Dùng cho trang "Tất cả thông báo" (Phân trang)
    Page<Notification> findByRecipientIdOrderByThoiGianGuiDesc(Long recipientId, Pageable pageable);

    // Dùng cho "Chuông thông báo" (Lấy 10 tin mới nhất, dạng List)
    List<Notification> findTop10ByRecipientIdOrderByThoiGianGuiDesc(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = ?1 AND n.isRead = false")
    void markAllAsReadByRecipientId(Long recipientId);
}