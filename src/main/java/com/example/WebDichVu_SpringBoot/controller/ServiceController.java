package com.example.WebDichVu_SpringBoot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.WebDichVu_SpringBoot.dto.ServiceRequest;
import com.example.WebDichVu_SpringBoot.entity.Category;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.service.CategoryService;
import com.example.WebDichVu_SpringBoot.service.CloudinaryService;
import com.example.WebDichVu_SpringBoot.service.ServiceService;
import com.example.WebDichVu_SpringBoot.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.ui.Model;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceController {

    private final ServiceService serviceService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    // @Value("${file.upload-dir:src/main/resources/static/assets/images}")
    // private String uploadDir;

    // Endpoint công khai cho khách hàng
    @GetMapping("/services")
    public ResponseEntity<Page<Service>> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(serviceService.findAllServices(page, size));
    }

    // Endpoint công khai cho khách hàng
    @GetMapping("/services/{id}")
    public ResponseEntity<Service> getServiceById(@PathVariable Long id) {
        return serviceService.findServiceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint công khai để lấy danh mục
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAllCategories());
    }

    // Endpoint admin
    @GetMapping("/admin/services")
    public ResponseEntity<Page<Service>> getAllServicesAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        return ResponseEntity.ok(serviceService.findAllServices(page, size));
    }

    @GetMapping("/admin/services/{id}")
    public ResponseEntity<Service> getServiceByIdAdmin(@PathVariable Long id) {
        return serviceService.findServiceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Dùng @ModelAttribute ServiceRequest để bind FormData
    @PostMapping(value = "/admin/services", consumes = { "multipart/form-data" })
    public ResponseEntity<Service> createService(@ModelAttribute ServiceRequest request) throws IOException {
        return categoryService.findCategoryById(request.getCategoryId())
                .map(category -> {
                    try {
                        Service service = new Service();
                        service.setTenDichVu(request.getTenDichVu().trim());
                        service.setGiaCoBan(BigDecimal.valueOf(request.getGiaCoBan()));
                        service.setThoiGianHoanThanh(request.getThoiGianHoanThanh());
                        service.setMoTa(request.getMoTa());
                        service.setCategory(category);

                        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                            String imageUrl = cloudinaryService.uploadImage(request.getImageFile());
                            service.setImageURL(imageUrl); // Lưu link https://... vào DB
                        }

                        // Gọi saveService → sẽ kiểm tra trùng tên trong ServiceService
                        Service savedService = serviceService.saveService(service);
                        return ResponseEntity.ok(savedService);
                    } catch (IOException e) {
                        throw new RuntimeException("Lỗi khi xử lý file ảnh: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.badRequest().body(null));
    }

    // Dùng @ModelAttribute ServiceRequest
    @PutMapping(value = "/admin/services/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<Service> updateService(
            @PathVariable Long id,
            @ModelAttribute ServiceRequest request) {

        return serviceService.findServiceById(id)
                .map(existingService -> categoryService.findCategoryById(request.getCategoryId())
                        .map(category -> {
                            try {

                                // Cập nhật thông tin cơ bản
                                existingService.setTenDichVu(request.getTenDichVu().trim());
                                existingService.setGiaCoBan(BigDecimal.valueOf(request.getGiaCoBan()));
                                existingService.setThoiGianHoanThanh(request.getThoiGianHoanThanh());
                                existingService.setMoTa(request.getMoTa());
                                existingService.setCategory(category);

                                // Nếu có chọn ảnh mới -> Upload lên Cloudinary -> Lấy link mới đè
                                if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                                    String imageUrl = cloudinaryService.uploadImage(request.getImageFile());
                                    existingService.setImageURL(imageUrl);
                                }

                                // Lưu vào Database
                                Service updatedService = serviceService.updateService(id, existingService);
                                return ResponseEntity.ok(updatedService);
                            } catch (IOException e) {
                                throw new RuntimeException("Lỗi upload ảnh lên Cloudinary: " + e.getMessage());
                            }
                        })
                        .orElse(ResponseEntity.badRequest().body(null)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        if (serviceService.findServiceById(id).isPresent()) {
            serviceService.deleteService(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint mới để render trang booking.html
    @GetMapping("/booking")
    public String showBookingPage(@RequestParam("service_id") Long serviceId, Model model) {
        return serviceService.findServiceById(serviceId)
                .map(service -> {
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    return userService.findByEmail(username)
                            .map(user -> {
                                model.addAttribute("service", service);
                                model.addAttribute("currentUser", user);
                                return "booking";
                            })
                            .orElseThrow(() -> new RuntimeException("User not found"));
                })
                .orElseThrow(() -> new RuntimeException("Service not found"));
    }

    // Bắt lỗi RuntimeException → trả JSON cho JS
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEntry(DataIntegrityViolationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "Tên dịch vụ đã tồn tại!");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @GetMapping("/admin/services/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Service>> getAllServicesList() {
        List<Service> services = serviceService.findAllServicesList();
        return ResponseEntity.ok(services);
    }
}