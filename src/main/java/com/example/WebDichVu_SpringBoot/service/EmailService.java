package com.example.WebDichVu_SpringBoot.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.WebDichVu_SpringBoot.entity.Contact;
import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    // Email người gửi
    private final String SENDER_EMAIL = "tuanhopq2019@gmail.com";
    private final String SENDER_NAME = "Dịch Vụ Tại Nhà";

    @Async
    public void sendOrderConfirmation(User user, Order order) {
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("order", order);
        String htmlContent = templateEngine.process("email/order-confirmation", context);

        sendHtmlEmail(user.getEmail(), "Xác nhận đặt lịch dịch vụ #" + order.getId(), htmlContent);
    }

    @Async
    public void sendPaymentSuccess(User user, Order order) {
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("order", order);
        String htmlContent = templateEngine.process("email/payment-success", context);

        sendHtmlEmail(user.getEmail(), "Thanh toán thành công đơn #" + order.getId(), htmlContent);
    }

    @Async
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // --- QUAN TRỌNG: THÊM DÒNG NÀY ---
            helper.setFrom(SENDER_EMAIL, SENDER_NAME);
            // ---------------------------------

            mailSender.send(message);
            log.info("Đã gửi email thành công đến: {}", to);
        } catch (Exception e) { // Bắt Exception chung để không bị sót lỗi
            log.error("Lỗi gửi email đến {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendStatusUpdate(User user, Order order, Order.Status oldStatus, Order.Status newStatus) {
        String statusText = getStatusVietnamese(newStatus);
        String subject = "Trạng thái đơn hàng #" + order.getId() + " đã thay đổi: " + statusText;

        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("order", order);
        context.setVariable("statusText", statusText);
        context.setVariable("oldStatus", getStatusVietnamese(oldStatus));

        String htmlContent = templateEngine.process("email/status-update", context);
        sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    private String getStatusVietnamese(Order.Status status) {
        return switch (status) {
            case CHUA_XU_LY -> "Chưa Xử Lý";
            case DA_NHAN -> "Đã Nhận";
            case HOAN_THANH -> "Hoàn Thành";
            case HUY -> "Đã Hủy";
        };
    }

    @Async
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            // --- QUAN TRỌNG: THÊM DÒNG NÀY ---
            helper.setFrom(SENDER_EMAIL, SENDER_NAME);
            // ---------------------------------

            mailSender.send(message);
        } catch (Exception e) { // Bắt Exception chung
            log.error("Lỗi gửi email text: {}", e.getMessage());
        }
    }

    @Async
    public void sendContactReply(Contact contact, String reply) {
        Context context = new Context();
        context.setVariable("contact", contact);
        context.setVariable("reply", reply);
        String htmlContent = templateEngine.process("email/contact-reply", context);

        sendHtmlEmail(
                contact.getEmail(),
                "Re: " + (contact.getSubject() != null ? contact.getSubject() : "Yêu cầu hỗ trợ"),
                htmlContent);
    }
}