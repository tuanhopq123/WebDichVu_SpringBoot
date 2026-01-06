package com.example.WebDichVu_SpringBoot.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.dto.AuthenticationResponse;
import com.example.WebDichVu_SpringBoot.dto.ChangePasswordRequest;
import com.example.WebDichVu_SpringBoot.dto.LoginRequest;
import com.example.WebDichVu_SpringBoot.dto.RegisterRequest;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.service.EmailService;
import com.example.WebDichVu_SpringBoot.service.JwtService;
import com.example.WebDichVu_SpringBoot.service.UserService;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // API Endpoint: POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User newUser = userService.registerUser(request);
            return new ResponseEntity<>("Đăng ký thành công với ID: " + newUser.getId(), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // API Endpoint: POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            // 1. Xác thực email và mật khẩu
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getMatKhau()));

            // 2. Lấy thông tin người dùng
            User user = userService.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng sau khi xác thực."));

            // 3. Tạo token
            String jwt = jwtService.generateToken(user);

            // 4. Trả về token, họ tên, và vai trò
            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .token(jwt)
                    .hoTen(user.getHoTen())
                    .vaiTro(user.getVaiTro().name())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sai email hoặc mật khẩu."));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> req, HttpServletRequest request) {
        String email = req.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email không được để trống"));
        }

        String otp = String.format("%04d", new Random().nextInt(10000));
        String key = "otp:" + email;

        try {
            // Lưu Redis
            redisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);
            log.info("OTP lưu Redis: {} = {}", key, otp);

            // Gửi email
            emailService.sendSimpleMessage(
                    email,
                    "Mã xác nhận đổi mật khẩu",
                    "Mã OTP của bạn là: " + otp + "\n\nHết hạn sau 5 phút.");
            log.info("Email OTP đã gửi đến: {}", email);

            return ResponseEntity.ok(Map.of("message", "OTP đã được gửi"));
        } catch (Exception e) {
            log.error("Lỗi gửi OTP: ", e);
            return ResponseEntity.status(500).body(Map.of("error", "Không thể gửi OTP: " + e.getMessage()));
        }
    }

    // === ĐỔI MẬT KHẨU ===
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req) {
        String email = req.getEmail();
        String otp = req.getOtp();
        String newPassword = req.getNewPassword();

        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu thông tin"));
        }

        String key = "otp:" + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null || !storedOtp.equals(otp)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mã OTP không đúng hoặc đã hết hạn"));
        }

        User user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy người dùng"));
        }

        // Cập nhật mật khẩu
        user.setMatKhau(passwordEncoder.encode(newPassword));
        userService.saveUser(user);

        // Xóa OTP
        redisTemplate.delete(key);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }
}