package com.example.WebDichVu_SpringBoot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.dto.NotificationDTO;
import com.example.WebDichVu_SpringBoot.entity.Notification;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // API 1: Lấy danh sách PHÂN TRANG (Dùng cho trang Xem tất cả)
    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationDTO>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User currentUser = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDTO> notifications = notificationService.getNotificationsForUser(currentUser.getId(),
                pageable);
        return ResponseEntity.ok(notifications);
    }

    // API 2: Lấy danh sách RÚT GỌN (Dùng cho cái Chuông)
    @GetMapping("/notifications/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getRecentNotifications(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<NotificationDTO> notifications = notificationService.getRecentNotifications(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        try {
            Notification notification = notificationService.markAsRead(id, currentUser.getId());
            Map<String, Object> response = Map.of("success", true, "link", notification.getLinkLienQuan());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAllNotificationsAsRead(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }
}