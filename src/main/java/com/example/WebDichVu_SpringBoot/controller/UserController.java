package com.example.WebDichVu_SpringBoot.controller;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import com.example.WebDichVu_SpringBoot.dto.EmployeeUpdateRequest;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.service.CloudinaryService;
import com.example.WebDichVu_SpringBoot.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    @Autowired
    public UserController(UserService userService, CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Page<User> userPage = userService.findAllUsers(page, size);
        userPage.getContent().forEach(user -> user.setMatKhau(null));
        return ResponseEntity.ok(userPage);
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllEmployees() {
        List<User> employees = userService.findEmployeesWithServices();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getEmployeeById(@PathVariable Long id) {
        return userService.findEmployeeByIdWithServices(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/employees/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEmployeeDetails(
            @PathVariable Long id,
            @RequestBody EmployeeUpdateRequest request) {

        try {
            User updatedEmployee = userService.updateEmployeeDetails(
                    id,
                    request.getSdt(),
                    request.getTrangThaiLamViec(),
                    request.getAssignedServiceId());
            return ResponseEntity.ok(updatedEmployee);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> getAllUsers(@RequestParam(defaultValue = "0") int page) {
        return userService.findAll(PageRequest.of(page, 100)); // Tối đa 100
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsersForAdmin() {
        // Hàm này nên trả về tất cả user, có thể bạn cần tạo nó trong UserService
        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            userService.toggleUserStatus(id);
            return ResponseEntity.ok().build();
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        System.out.println("DEBUG CONTROLLER /ME: Request authenticated for user: " + username);
        Optional<User> userOptional = userService.findByEmail(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setMatKhau(null);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userService.findUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setMatKhau(null);
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(
            @RequestParam("hoTen") String hoTen,
            @RequestParam("email") String email,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("vaiTro") String vaiTroStr) {

        if (userService.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email đã tồn tại."));
        }
        if (matKhau.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu phải có ít nhất 6 ký tự."));
        }

        User user = new User();
        user.setHoTen(hoTen);
        user.setEmail(email);
        user.setVaiTro(User.Role.valueOf(vaiTroStr.toUpperCase()));
        user.setMatKhau(userService.encodePassword(matKhau));

        User savedUser = userService.saveUser(user);
        savedUser.setMatKhau(null);
        return ResponseEntity.ok(savedUser);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestParam("hoTen") String hoTen,
            @RequestParam("email") String email,
            @RequestParam(value = "matKhau", required = false) String matKhau,
            @RequestParam("vaiTro") String vaiTroStr) {

        Optional<User> existingUserOpt = userService.findUserById(id);
        if (!existingUserOpt.isPresent()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Không tìm thấy người dùng."));
        }

        User existingUser = existingUserOpt.get();
        if (!existingUser.getEmail().equals(email) && userService.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email đã tồn tại."));
        }
        if (matKhau != null && matKhau.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu phải có ít nhất 6 ký tự."));
        }

        existingUser.setHoTen(hoTen);
        existingUser.setEmail(email);
        if (matKhau != null && !matKhau.isEmpty()) {
            existingUser.setMatKhau(userService.encodePassword(matKhau));
        }
        existingUser.setVaiTro(User.Role.valueOf(vaiTroStr.toUpperCase()));

        User updatedUser = userService.saveUser(existingUser);
        updatedUser.setMatKhau(null);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // NGƯỜI DÙNG: CẬP NHẬT THÔNG TIN CÁ NHÂN
    @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, String> updates) {

        String email = authentication.getName();
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy người dùng"));
        }

        User user = userOptional.get();

        if (updates.containsKey("hoTen")) {
            user.setHoTen(updates.get("hoTen"));
        }
        if (updates.containsKey("sdt")) {
            user.setSdt(updates.get("sdt"));
        }
        if (updates.containsKey("diaChi")) {
            user.setDiaChi(updates.get("diaChi"));
        }

        userService.saveUser(user);

        return ResponseEntity.ok(Map.of("message", "Cập nhật thành công"));
    }

    // NGƯỜI DÙNG: UPLOAD AVATAR
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOptional = userService.findByEmail(email);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy user"));
            }
            User user = userOptional.get();

            // 1. Upload lên Cloudinary
            String avatarUrl = cloudinaryService.uploadImage(file);

            // 2. Lưu link https://... vào Database
            user.setAvatarURL(avatarUrl);
            userService.saveUser(user);

            // 3. Trả về JSON key "avatarURL" (Viết hoa URL) để khớp với Frontend
            return ResponseEntity.ok(Map.of("avatarURL", avatarUrl));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi upload ảnh: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/employees/available")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAvailableEmployees(
            @RequestParam("serviceId") Long serviceId) { // <-- THÊM @RequestParam

        if (serviceId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng cung cấp serviceId"));
        }

        try {
            // Gọi hàm service đã sửa đổi
            List<User> employees = userService.findAvailableEmployeesForService(serviceId);
            return ResponseEntity.ok(employees);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}