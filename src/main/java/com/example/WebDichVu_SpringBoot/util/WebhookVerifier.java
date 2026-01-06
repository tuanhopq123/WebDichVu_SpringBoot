package com.example.WebDichVu_SpringBoot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class WebhookVerifier {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  // Hàm sắp xếp key theo thứ tự Alphabet
  public static Map<String, Object> sortObjDataByKey(Map<String, Object> data) {
    Map<String, Object> sortedMap = new TreeMap<>();
    for (String key : data.keySet()) {
      Object value = data.get(key);
      if (value instanceof Map) {
        // Đệ quy nếu là đối tượng lồng nhau
        sortedMap.put(key, sortObjDataByKey((Map<String, Object>) value));
      } else {
        sortedMap.put(key, value);
      }
    }
    return sortedMap;
  }

  // Hàm kiểm tra chữ ký HMAC-SHA512
  public static boolean verifyWebhookSignature(String receivedSignature, Map<String, Object> data, String checksumKey) {
    try {
      // 1. Tách Timestamp và Signature
      String[] signatureParts = receivedSignature.split(",");
      String timestampStr = signatureParts[0].split("=")[1];
      String signature = signatureParts[1].split("=")[1];
      long timestamp = Long.parseLong(timestampStr);

      // 2. Sắp xếp JSON data theo Key
      Map<String, Object> sortedDataByKey = sortObjDataByKey(data);

      // 3. Chuyển Dữ liệu thành Chuỗi JSON
      String jsonPayload = objectMapper.writeValueAsString(sortedDataByKey);

      // 4. Tạo message để ký: timestamp + "." + JSON
      String messageToSign = timestamp + "." + jsonPayload;

      // 5. Tính toán HMAC-SHA512
      Mac hmacSha512 = Mac.getInstance("HmacSHA512");
      SecretKeySpec secretKeySpec = new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
      hmacSha512.init(secretKeySpec);
      byte[] hash = hmacSha512.doFinal(messageToSign.getBytes(StandardCharsets.UTF_8));

      // 6. Chuyển Hash thành chuỗi Hex
      StringBuilder generatedSignature = new StringBuilder();
      for (byte b : hash) {
        generatedSignature.append(String.format("%02x", b));
      }

      // 7. So sánh chữ ký
      return signature.equals(generatedSignature.toString());
    } catch (Exception e) {
      System.err.println("Lỗi xác thực chữ ký Casso: " + e.getMessage());
      return false;
    }
  }
}