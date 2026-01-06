package com.example.WebDichVu_SpringBoot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.WebDichVu_SpringBoot.dto.RegisterRequest;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.ServiceRepository;
import com.example.WebDichVu_SpringBoot.repository.UserRepository;

import org.springframework.security.oauth2.core.user.OAuth2User;

import jakarta.persistence.EntityNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@org.springframework.stereotype.Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceRepository serviceRepository;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            ServiceRepository serviceRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.serviceRepository = serviceRepository;
    }

    public Page<User> findAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    public Page<User> findAll(PageRequest pageRequest) {
        return userRepository.findAll(pageRequest);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(
                        () -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy user với ID: " + id));

        // Đảo ngược trạng thái
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    // Lấy tất cả user (không phân trang) - dùng cho admin
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại.");
        }
        if (request.getMatKhau().length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự.");
        }

        User user = new User();
        user.setHoTen(request.getHoTen());
        user.setEmail(request.getEmail());
        user.setMatKhau(passwordEncoder.encode(request.getMatKhau()));
        user.setVaiTro(User.Role.KHACH);
        user.setIsEnabled(true);
        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));
        System.out.println(
                "DEBUG ROLE CHECK: User [" + user.getEmail() + "] loaded with Role: " + user.getVaiTro().name());
        return user;
    }

    public User saveUser(User user) {
        // Kiểm tra email tồn tại nếu thêm mới (id null)
        if (user.getId() == null && userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã tồn tại.");
        }
        // Chỉ mã hóa mật khẩu nếu nó được cung cấp, không rỗng, và chưa được mã hóa
        if (user.getMatKhau() != null && !user.getMatKhau().isEmpty() && !user.getMatKhau().startsWith("$2a$")) {
            if (user.getMatKhau().length() < 6) {
                throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự.");
            }
            user.setMatKhau(passwordEncoder.encode(user.getMatKhau()));
        }
        return userRepository.save(user);
    }

    public void deleteById(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        } else {
            throw new RuntimeException("User không tồn tại với ID: " + id);
        }
    }

    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống.");
        }
        if (rawPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        return passwordEncoder.encode(rawPassword);
    }

    // 🔹 THÊM MỚI: Method xử lý upload và lưu avatar
    @Transactional
    public String updateAvatar(MultipartFile file, Long userId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được rỗng");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, GIF)");
        }
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("Kích thước file vượt quá 5MB");
        }

        // Tìm user
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy user");
        }
        User user = optionalUser.get();

        // Tạo tên file unique: userId_timestamp_uuid.ext
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = contentType.substring(contentType.lastIndexOf("/") + 1); // jpeg, png, gif
        String filename = userId + "_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + "."
                + extension;

        // Đường dẫn thư mục: src/main/resources/static/assets/avatar (cho dev với
        // Laragon)
        // Khi build JAR, file sẽ bundle vào classpath static
        Path uploadDir = Paths.get("src/main/resources/static/assets/avatar");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path filePath = uploadDir.resolve(filename);

        // Lưu file (thay thế nếu trùng tên, nhưng unique nên hiếm)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Xóa file cũ nếu có (tránh rác, trừ default)
        if (user.getAvatarURL() != null && !user.getAvatarURL().equals("/assets/avatar/default-avatar.png")) {
            String oldFilename = user.getAvatarURL().substring(user.getAvatarURL().lastIndexOf("/") + 1);
            Path oldPath = uploadDir.resolve(oldFilename);
            if (Files.exists(oldPath)) {
                Files.delete(oldPath);
            }
        }

        // Cập nhật URL relative cho static serve: /assets/avatar/filename
        user.setAvatarURL("/assets/avatar/" + filename);
        userRepository.save(user); // Save với @Transactional

        return user.getAvatarURL(); // Trả uppercase để match entity JSON
    }

    // DÁN CODE NÀY VÀO UserService.java
    @Transactional
    public User processOAuthUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // --- SỬA LỖI TẠI ĐÂY ---

            // 1. KIỂM TRA TRẠNG THÁI KHÓA
            // Nếu tài khoản đã tồn tại và bị khóa (isEnabled == false)
            if (!user.isEnabled()) {
                // Ném ra ngoại lệ OAuth2AuthenticationException để Spring Security bắt được và
                // báo lỗi
                throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("account_disabled"),
                        "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
            }

            // 2. CẬP NHẬT THÔNG TIN (Nhưng KHÔNG ĐƯỢC setEnabled(true))
            if (user.getProvider() == null) {
                user.setProvider("google");
            }
            user.setAvatarURL(picture);
            // user.setEnabled(true); <--- XÓA DÒNG NÀY ĐI (Đây là thủ phạm tự mở khóa)

            return userRepository.save(user);

        } else {
            // --- TRƯỜNG HỢP TẠO MỚI ---
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setHoTen(name != null ? name : email.split("@")[0]);
            newUser.setAvatarURL(picture);
            newUser.setProvider("google");
            newUser.setVaiTro(User.Role.KHACH);

            // User mới đăng ký thì mặc định cho phép hoạt động
            newUser.setEnabled(true);

            return userRepository.save(newUser);
        }
    }

    // Thêm phương thức này vào UserService
    public long countAllUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public List<User> findEmployeesWithServices() {
        List<User> employees = userRepository.findUsersByVaiTroWithServices(User.Role.NHAN_VIEN);

        // Loại bỏ mật khẩu trước khi trả về
        employees.forEach(emp -> emp.setMatKhau(null));
        return employees;
    }

    @Transactional(readOnly = true)
    public Optional<User> findEmployeeByIdWithServices(Long id) {
        Optional<User> employeeOpt = userRepository.findByIdAndVaiTroWithServices(id, User.Role.NHAN_VIEN);

        // Loại bỏ mật khẩu
        employeeOpt.ifPresent(emp -> emp.setMatKhau(null));
        return employeeOpt;
    }

    @Transactional
    public User updateEmployeeDetails(Long employeeId, String sdt, String trangThaiStr, Long assignedServiceId) { // THAY
                                                                                                                  // ĐỔI
                                                                                                                  // 1

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Không tìm thấy nhân viên với ID: " + employeeId));

        if (employee.getVaiTro() != User.Role.NHAN_VIEN) {
            throw new IllegalArgumentException("Người dùng (ID: " + employeeId + ") không phải là nhân viên.");
        }

        employee.setSdt(sdt);

        try {
            employee.setTrangThaiLamViec(User.TrangThaiLamViec.valueOf(trangThaiStr.toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Trạng thái nhân viên không hợp lệ: " + trangThaiStr);
        }

        // THAY ĐỔI 2: Toàn bộ logic cập nhật dịch vụ
        Set<Service> assignedServices = new HashSet<>();

        // Nếu ID dịch vụ được cung cấp (khác null và > 0)
        if (assignedServiceId != null && assignedServiceId > 0) {
            // Tìm 1 dịch vụ duy nhất
            Service service = serviceRepository.findById(assignedServiceId)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                            "Không tìm thấy dịch vụ với ID: " + assignedServiceId));
            assignedServices.add(service);
        }
        // Nếu assignedServiceId là null,
        // assignedServices sẽ là Set rỗng (đúng ý đồ gỡ bỏ)

        employee.setDichVuDamNhan(assignedServices);

        User savedEmployee = userRepository.save(employee);
        savedEmployee.setMatKhau(null);
        return savedEmployee;
    }

    public List<User> findAvailableEmployeesForService(Long serviceId) {
        // 1. Tìm đối tượng Service
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy dịch vụ ID: " + serviceId));

        // 2. Gọi phương thức repository mới
        List<User> availableEmployees = userRepository.findByVaiTroAndTrangThaiLamViecAndDichVuDamNhanContains(
                User.Role.NHAN_VIEN,
                User.TrangThaiLamViec.RANH,
                service);

        // 3. Xóa mật khẩu trước khi trả về
        availableEmployees.forEach(user -> user.setMatKhau(null));
        return availableEmployees;
    }
}