package com.example.WebDichVu_SpringBoot.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.entity.Contact;
import com.example.WebDichVu_SpringBoot.service.ContactService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép gọi từ mọi nơi
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/contact")
    public ResponseEntity<?> createContact(@RequestBody Contact contact) {
        try {
            // Kiểm tra dữ liệu đầu vào cơ bản (Validate)
            if (contact.getEmail() == null || contact.getMessage() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email và nội dung không được để trống"));
            }

            // Lưu vào DB
            contactService.saveContact(contact);

            return ResponseEntity.ok(Map.of("message", "Gửi liên hệ thành công!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/contacts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String search) {

        Page<Contact> result = contactService.getContacts(page, size, filter, search);
        long unreadCount = contactService.countUnread();

        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "pageable", result.getPageable(),
                "unreadCount", unreadCount));
    }

    @PostMapping("/admin/contacts/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> replyContact(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            contactService.replyAndSendEmail(id, body.get("reply"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
