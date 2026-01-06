package com.example.WebDichVu_SpringBoot.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.WebDichVu_SpringBoot.dto.NotificationDTO;
import com.example.WebDichVu_SpringBoot.entity.Notification;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // Lấy danh sách phân trang (cho trang notifications.html)
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotificationsForUser(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByRecipientIdOrderByThoiGianGuiDesc(userId, pageable);
        return page.map(NotificationDTO::new);
    }

    // Lấy danh sách rút gọn cho chuông (cho notification.js)
    @Transactional(readOnly = true)
    public List<NotificationDTO> getRecentNotifications(Long userId) {
        List<Notification> list = notificationRepository.findTop10ByRecipientIdOrderByThoiGianGuiDesc(userId);
        return list.stream().map(NotificationDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public void createNotification(User recipient, String tieuDe, String noiDung, String link, Notification.Type type) {
        if (recipient == null)
            return;
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTieuDe(tieuDe);
        notification.setNoiDung(noiDung);
        notification.setLinkLienQuan(link);
        notification.setType(type);
        notification.setRead(false);
        notification.setThoiGianGui(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông báo"));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập thông báo này");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByRecipientId(userId);
    }
}