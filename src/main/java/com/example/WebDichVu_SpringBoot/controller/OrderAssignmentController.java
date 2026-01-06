package com.example.WebDichVu_SpringBoot.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.OrderAssignment;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.service.OrderAssignmentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderAssignmentController {

    private final OrderAssignmentService assignmentService;

    /**
     * API cho nhân viên lấy danh sách lời mời (đang chờ) của chính mình
     */
    @GetMapping("/my-invitations")
    @PreAuthorize("hasRole('NHAN_VIEN')")
    public ResponseEntity<List<OrderAssignment>> getMyPendingAssignments(Authentication authentication) {
        User employee = (User) authentication.getPrincipal();
        List<OrderAssignment> assignments = assignmentService.getPendingAssignments(employee.getId());
        return ResponseEntity.ok(assignments);
    }

    /**
     * API cho nhân viên chấp nhận một lời mời
     */
    @PostMapping("/{assignmentId}/accept")
    @PreAuthorize("hasRole('NHAN_VIEN')")
    public ResponseEntity<?> acceptAssignment(@PathVariable Long assignmentId, Authentication authentication) {
        User employee = (User) authentication.getPrincipal();
        try {
            assignmentService.acceptAssignment(assignmentId, employee);

            return ResponseEntity.ok(Map.of(
                    "message", "Nhận việc thành công!",
                    "status", "ACCEPTED"));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }

    /**
     * API cho nhân viên từ chối một lời mời
     */
    @PostMapping("/{assignmentId}/reject")
    @PreAuthorize("hasRole('NHAN_VIEN')")
    public ResponseEntity<?> rejectAssignment(@PathVariable Long assignmentId, Authentication authentication) {
        User employee = (User) authentication.getPrincipal();
        try {
            assignmentService.rejectAssignment(assignmentId, employee);

            return ResponseEntity.ok(Map.of(
                    "message", "Đã từ chối lời mời.",
                    "status", "REJECTED"));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('NHAN_VIEN')")
    public ResponseEntity<List<OrderAssignment>> getMyHistory(Authentication authentication) {
        User employee = (User) authentication.getPrincipal();
        List<OrderAssignment> history = assignmentService.getHistoryForEmployee(employee.getId());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/payment-confirmations")
    @PreAuthorize("hasRole('NHAN_VIEN')")
    public ResponseEntity<List<Order>> getPaymentConfirmations(Authentication authentication) {
        User employee = (User) authentication.getPrincipal();
        List<Order> orders = assignmentService.getPendingPaymentConfirmations(employee.getId());
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/payment-confirmations/{orderId}/confirm")
    @PreAuthorize("hasRole('NHAN_VIEN')")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long orderId,
            Authentication authentication) {

        User employee = (User) authentication.getPrincipal();
        try {
            assignmentService.confirmPayment(orderId, employee.getId());
            return ResponseEntity.ok(Map.of("message", "Xác nhận thành công."));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}