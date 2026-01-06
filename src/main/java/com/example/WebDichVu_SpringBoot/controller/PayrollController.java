package com.example.WebDichVu_SpringBoot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.service.PayrollService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/summary")
    public ResponseEntity<List<Map<String, Object>>> getPayrollSummary() {
        return ResponseEntity.ok(payrollService.getPayrollSummary());
    }

    @GetMapping("/details/{employeeId}")
    public ResponseEntity<List<Order>> getUnpaidOrderDetails(
            @PathVariable Long employeeId,
            @RequestParam String fromDate,
            @RequestParam String toDate) {

        // Parse ngày tháng
        LocalDate start = LocalDate.parse(fromDate);
        LocalDate end = LocalDate.parse(toDate);

        return ResponseEntity.ok(payrollService.getUnpaidOrdersForEmployee(employeeId, start, end));
    }

    // Request Payment
    @PostMapping("/request-payment")
    public ResponseEntity<?> requestPayment(@RequestBody Map<String, Object> payload) {
        try {
            Long employeeId = Long.parseLong(payload.get("employeeId").toString());
            LocalDate fromDate = LocalDate.parse(payload.get("fromDate").toString());
            LocalDate toDate = LocalDate.parse(payload.get("toDate").toString());

            payrollService.requestPaymentForOrders(employeeId, fromDate, toDate);
            return ResponseEntity.ok(Map.of("message", "Thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Order>> getHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        LocalDateTime start = (fromDate != null && !fromDate.isEmpty()) ? LocalDate.parse(fromDate).atStartOfDay()
                : null;
        LocalDateTime end = (toDate != null && !toDate.isEmpty()) ? LocalDate.parse(toDate).atTime(23, 59, 59) : null;

        return ResponseEntity.ok(payrollService.getHistory(search, status, start, end));
    }

}