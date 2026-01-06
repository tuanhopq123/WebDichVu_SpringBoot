package com.example.WebDichVu_SpringBoot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatbotController {

        // DỮ LIỆU DỊCH VỤ GỢI Ý (MẪU)
        private static final List<Map<String, String>> SERVICE_SUGGESTIONS = List.of(
                        Map.of(
                                        "name", "Vệ sinh máy lạnh",
                                        "desc", "Làm sạch toàn bộ, khử mùi, bảo hành 7 ngày",
                                        "price", "150k - 220k"),
                        Map.of(
                                        "name", "Sửa điện nước",
                                        "desc", "Sửa ống nước, thay ổ cắm, sửa quạt trần",
                                        "price", "100k - 300k"),
                        Map.of(
                                        "name", "Làm nail tại nhà",
                                        "desc", "Sơn gel, đính đá, chăm sóc móng tay",
                                        "price", "120k - 180k"));

        private record Intent(String[] keywords, String reply) {
        }

        // API: XỬ LÝ TIN NHẮN TỪ CHATBOT
        @PostMapping("/chatbot")
        public ResponseEntity<Map<String, Object>> handleMessage(@RequestBody Map<String, String> request) {
                String message = request.get("message");
                if (message == null || message.trim().isEmpty()) {
                        return ResponseEntity.ok(Map.of("reply", "Xin lỗi, tôi không nhận được tin nhắn."));
                }

                String msg = message.toLowerCase().trim();
                if (containsAny(msg, "dịch vụ", "có gì", "gợi ý", "xem dịch vụ")) {
                        return ResponseEntity.ok(Map.of("reply", buildServiceSuggestions()));
                }
                String reply = getBotReply(msg);
                return ResponseEntity.ok(Map.of("reply", reply));
        }

        // TẠO HTML GỢI Ý DỊCH VỤ (ĐẸP, DỄ ĐỌC TRONG CHAT)
        private String buildServiceSuggestions() {
                StringBuilder sb = new StringBuilder();
                sb.append("<div style='font-size:14px; line-height:1.5;'>");
                sb.append("<strong>Dịch vụ nổi bật hôm nay:</strong><br><br>");

                for (Map<String, String> service : SERVICE_SUGGESTIONS) {
                        sb.append("<div style='margin-bottom:12px; padding:10px; border-left:3px solid #007bff; ")
                                        .append("background:#f8f9fa; border-radius:4px;'>")
                                        .append("<strong>").append(service.get("name")).append("</strong><br>")
                                        .append("<small style='color:#555;'>").append(service.get("desc"))
                                        .append("</small><br>")
                                        .append("<span style='color:#d63939; font-weight:600;'>")
                                        .append(service.get("price")).append("</span>")
                                        .append("</div>");
                }

                sb.append("<br><a href='/services.html' ")
                                .append("style='color:#007bff; font-weight:500; text-decoration:underline;'>")
                                .append("Xem tất cả dịch vụ</a>");
                sb.append("</div>");
                return sb.toString();
        }

        // PHÂN LOẠI Ý ĐỊNH
        private String getBotReply(String msg) {
                List<Intent> intents = List.of(
                                new Intent(
                                                new String[] { "xem", "muốn xem", "xem các", "dịch vụ", "có dịch vụ",
                                                                "dịch vụ gì", "có gì" },
                                                "Dạ, em mời anh/chị xem danh sách dịch vụ tại đây nhé! " +
                                                                "<a href='/services.html' style='color:#007bff; font-weight:600;'>Xem ngay</a>"),
                                new Intent(
                                                new String[] { "đặt", "booking", "lịch", "đặt lịch", "muốn đặt",
                                                                "đặt dịch vụ" },
                                                "Dạ, anh/chị chọn dịch vụ và đặt lịch ngay tại đây ạ: " +
                                                                "<a href='/services.html' style='color:#007bff; font-weight:600;'>Đặt lịch</a>"),
                                new Intent(
                                                new String[] { "giá", "bảng giá", "bao nhiêu", "chi phí", "tiền" },
                                                "Dạ, bảng giá chi tiết đây ạ: " +
                                                                "<a href='/service_price_list.html' style='color:#007bff; font-weight:600;'>Xem bảng giá</a>"),
                                new Intent(
                                                new String[] { "liên hệ", "hỗ trợ", "gọi", "số điện thoại", "hotline",
                                                                "email" },
                                                "Dạ, anh/chị liên hệ ngay:<br>" +
                                                                "<strong>Hotline:</strong> 0783 998 046<br>" +
                                                                "<strong>Email:</strong> <a href='mailto:tuanhopq2019@gmail.com'>tuanhopq2019@gmail.com</a>"),
                                new Intent(
                                                new String[] { "giờ", "mở cửa", "làm việc", "thời gian", "mấy giờ" },
                                                "Dạ, chúng tôi làm việc:<br>" +
                                                                "<strong>Thứ 2 - Chủ Nhật:</strong> 8:00 - 20:00"),
                                new Intent(
                                                new String[] { "lịch sử", "đơn cũ", "đã đặt", "đánh giá", "trạng thái",
                                                                "theo dõi" },
                                                "Dạ, anh/chị xem lịch sử đặt dịch vụ tại đây: " +
                                                                "<a href='/my_bookings.html' style='color:#007bff; font-weight:600;'>Xem đơn</a>"),
                                new Intent(
                                                new String[] { "địa chỉ", "ở đâu", "vị trí", "địa điểm", "nơi" },
                                                "Dạ, địa chỉ: <strong>Thủ Đức, TP.HCM</strong><br>" +
                                                                "Xem bản đồ: <a href='https://maps.google.com/?q=Th%E1%BB%A7+%C4%90%E1%BB%A9c,+TP.HCM' "
                                                                +
                                                                "target='_blank' style='color:#007bff;'>Mở Google Maps</a>"),
                                new Intent(
                                                new String[] { "chào", "xin chào", "hello", "hi", "alo", "hey" },
                                                "Dạ, chào anh/chị! Em là trợ lý ảo của <strong>Dịch vụ Tại Nhà</strong>. "
                                                                +
                                                                "Anh/chị cần hỗ trợ gì ạ?"),

                                // 9. CẢM ƠN
                                new Intent(
                                                new String[] { "cảm ơn", "cám ơn", "thanks", "ok", "tốt" },
                                                "Dạ, không có chi ạ! Anh/chị cần hỗ trợ thêm gì không?"));

                // Tìm ý định có điểm cao nhất (ưu tiên từ khóa dài hơn)
                String bestReply = null;
                int bestScore = 0;

                for (Intent intent : intents) {
                        int score = 0;
                        for (String keyword : intent.keywords) {
                                if (msg.contains(keyword)) {
                                        score += keyword.length(); // Điểm dựa trên độ dài từ khóa
                                }
                        }
                        if (score > bestScore) {
                                bestReply = intent.reply;
                                bestScore = score;
                        }
                }

                // Phản hồi mặc định nếu không hiểu
                return (bestReply != null)
                                ? bestReply
                                : "Dạ, anh/chị cho em biết thêm để em hỗ trợ tốt hơn nhé!<br>" +
                                                "Gọi ngay: <strong style='color:#007bff;'>0783 998 046</strong>";
        }

        // HÀM HỖ TRỢ: Kiểm tra tin nhắn có chứa bất kỳ từ nào không
        private boolean containsAny(String text, String... keywords) {
                for (String keyword : keywords) {
                        if (text.contains(keyword)) {
                                return true;
                        }
                }
                return false;
        }
}