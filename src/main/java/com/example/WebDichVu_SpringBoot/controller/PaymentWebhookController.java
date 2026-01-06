package com.example.WebDichVu_SpringBoot.controller;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.WebDichVu_SpringBoot.dto.CassoPayload;
import com.example.WebDichVu_SpringBoot.dto.CassoTransaction;
import com.example.WebDichVu_SpringBoot.service.OrderService;
import com.example.WebDichVu_SpringBoot.util.WebhookVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

  private final OrderService orderService;
  private final ObjectMapper objectMapper;

  @Value("${casso.api.key}")
  private String CASSO_API_KEY;

  @PostMapping("/casso")
  public ResponseEntity<?> handleCassoWebhook(
      @RequestHeader(value = "X-Casso-Signature", required = false) String cassoSignature,
      @RequestBody Map<String, Object> rawPayload) {

    // 1. KIỂM TRA BẢO MẬT
    if (cassoSignature == null ||
        !WebhookVerifier.verifyWebhookSignature(cassoSignature, rawPayload,
            CASSO_API_KEY)) {
      System.err.println("LỖI BẢO MẬT: Chữ ký Casso không hợp lệ hoặc thiếu.");
      return ResponseEntity.status(403).body("Invalid X-Casso-Signature");
    }

    // 2. Map Payload trở lại thành DTO
    CassoPayload payload;
    try {
      payload = objectMapper.convertValue(rawPayload, CassoPayload.class);
    } catch (Exception e) {
      System.err.println("Lỗi mapping JSON: " + e.getMessage());
      return ResponseEntity.badRequest().body("Invalid Payload Structure");
    }

    // 3. Xử lý logic
    CassoTransaction trans = payload.getData();

    if (trans != null) {
      String description = trans.getDescription();
      BigDecimal amount = trans.getAmount();

      // LẤY TẤT CẢ CÁC ID TỪ NỘI DUNG CHUYỂN KHOẢN
      List<Long> orderIds = extractAllOrderIds(description);

      if (!orderIds.isEmpty()) {
        try {
          // GỌI SERVICE CHỈ MỘT LẦN VỚI DANH SÁCH ID
          orderService.confirmBatchPayment(orderIds, amount);
          System.out.println("Webhook THÀNH CÔNG: Đã xử lý thanh toán batch cho các đơn: " + orderIds);
        } catch (Exception e) {
          System.err.println("Lỗi xử lý batch cho các đơn " + orderIds + ": " + e.getMessage());
        }
      }
    }

    return ResponseEntity.ok(rawPayload);
  }

  // Hàm tách ID "dễ tính": Chấp nhận dấu cách, dấu phẩy, gạch ngang
  private List<Long> extractAllOrderIds(String text) {
    if (text == null)
      return Collections.emptyList();
    List<Long> ids = new ArrayList<>();
    try {
      Pattern pattern = Pattern.compile("(?i)PAY([\\d\\-,\\s]+)T");
      Matcher matcher = pattern.matcher(text);

      if (matcher.find()) {
        String idGroup = matcher.group(1);

        String[] idStrings = idGroup.split("[^\\d]+");

        for (String idStr : idStrings) {
          // Vì cắt bằng regex có thể sinh ra chuỗi rỗng, cần check
          if (!idStr.trim().isEmpty()) {
            ids.add(Long.parseLong(idStr.trim()));
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Lỗi tách ID: " + e.getMessage());
    }
    return ids;
  }
}