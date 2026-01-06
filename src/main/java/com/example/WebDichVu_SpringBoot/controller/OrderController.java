package com.example.WebDichVu_SpringBoot.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.dto.CartOrderRequest;
import com.example.WebDichVu_SpringBoot.dto.OrderRequest;
import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.OrderAssignment;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.UserRepository;
import com.example.WebDichVu_SpringBoot.service.CartService;
import com.example.WebDichVu_SpringBoot.service.OrderService;
import com.example.WebDichVu_SpringBoot.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private CartService cartService;

    @Autowired
    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    // GET /api/orders/{id}: Lấy đơn hàng theo ID (KHÁCH HÀNG)
    @PreAuthorize("hasRole('KHACH') or hasRole('ADMIN')")
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        try {
            Order order = orderService.findOrderById(id);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            // Kiểm tra nếu user không phải ADMIN và không phải chủ đơn hàng
            if (!order.getUser().getEmail().equals(userEmail) &&
                    !authentication.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(order);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/admin/orders: Lấy tất cả đơn hàng (ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/orders")
    public ResponseEntity<Page<Order>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(orderService.findAllOrders(page, size));
    }

    @GetMapping("/admin/orders/all")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> allOrders = orderService.findAllOrdersForAdmin();
        return ResponseEntity.ok(allOrders);
    }

    // GET /api/admin/orders/{id}: Lấy đơn hàng theo ID (ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/orders/{id}")
    public ResponseEntity<Order> getOrderByIdAdmin(@PathVariable Long id) {
        try {
            Order order = orderService.findOrderById(id);
            return ResponseEntity.ok(order);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * API Endpoint cho khách hàng tự lấy lịch sử đơn hàng của CHÍNH MÌNH.
     * Khớp với lời gọi fetch('/api/my-orders') từ my_bookings.html.
     *
     * @param authentication Spring Security tự động tiêm thông tin người dùng đã
     *                       đăng nhập
     * @return Danh sách đơn hàng của người dùng đó
     */
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('KHACH')") // Chỉ KHACH mới được gọi API này
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {

        // Lấy thông tin User đã được xác thực từ JWT
        User userDetails = (User) authentication.getPrincipal();
        Long userId = userDetails.getId();

        try {
            // Gọi service để lấy danh sách đơn hàng
            List<Order> orders = orderService.findOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Trường hợp user ID từ token không tồn tại trong DB (rất hiếm)
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/orders: Tạo đơn hàng mới (KHÁCH HÀNG)
    @PostMapping("/orders")
    @PreAuthorize("hasRole('KHACH')")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest orderRequest, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        try {
            Order newOrder = orderService.createOrder(
                    currentUser,
                    orderRequest.getServiceId(),
                    orderRequest.getThoiGianDat(),
                    orderRequest.getDiaChiDichVu(),
                    orderRequest.getTongTien(),
                    orderRequest.getPhuongThucThanhToan(),
                    orderRequest.getTrangThai(),
                    orderRequest.getNotes(),
                    orderRequest.getSdt());
            return ResponseEntity.status(201).body(newOrder);
        } catch (jakarta.persistence.EntityNotFoundException | IllegalArgumentException e) {
            System.err.println("Lỗi khi tạo đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi tạo đơn hàng: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // PUT /api/admin/orders/{id}: Cập nhật toàn bộ đơn hàng (ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/orders/{id}")
    public ResponseEntity<Void> updateOrder(
            @PathVariable Long id,
            @RequestBody OrderRequest orderRequest) {
        try {
            orderService.updateOrder(
                    id,
                    orderRequest.getServiceId(),
                    orderRequest.getThoiGianDat(),
                    orderRequest.getDiaChiDichVu(),
                    orderRequest.getTongTien(),
                    orderRequest.getPhuongThucThanhToan(),
                    orderRequest.getTrangThai(),
                    orderRequest.getSdt(),
                    orderRequest.getPaymentStatus());
            return ResponseEntity.ok().build();
        } catch (jakarta.persistence.EntityNotFoundException | IllegalArgumentException e) {
            System.err.println("Lỗi khi cập nhật đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi cập nhật đơn hàng: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // PUT /api/admin/orders/{id}/status: Cập nhật trạng thái đơn hàng (ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Order updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (jakarta.persistence.EntityNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> createOrderByAdmin(@RequestBody OrderRequest orderRequest) {
        try {
            User user = userService.findUserById(orderRequest.getUserId())
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

            Order newOrder = orderService.createOrder(
                    user,
                    orderRequest.getServiceId(),
                    orderRequest.getThoiGianDat(),
                    orderRequest.getDiaChiDichVu(),
                    orderRequest.getTongTien(),
                    orderRequest.getPhuongThucThanhToan(),
                    orderRequest.getTrangThai(),
                    orderRequest.getNotes(),
                    orderRequest.getSdt());
            return ResponseEntity.status(201).body(newOrder);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(404).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/admin/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestParam String type,
            @RequestParam(required = false) String value) {

        Map<String, Object> result = new HashMap<>();

        // Tổng người dùng
        long totalUsers = userService.countAllUsers();
        result.put("totalUsers", totalUsers);

        // Doanh thu theo thời gian

        // Gọi hàm service đã thay đổi
        Map<String, Object> revenueResult = orderService.getRevenueByPeriod(type, value);

        // Giải nén Map từ service vào 'result' chính
        result.put("revenueData", revenueResult.get("revenueData"));
        result.put("totalRevenue", revenueResult.get("totalRevenue"));

        List<Map<String, Object>> topServices = orderService.getTopServices(type, value);
        result.put("topServices", topServices);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/orders/{orderId}/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> inviteEmployees(
            @PathVariable Long orderId,
            @RequestBody Map<String, List<Long>> request) {

        List<Long> employeeIds = request.get("employeeIds");
        if (employeeIds == null || employeeIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Danh sách nhân viên không được rỗng"));
        }

        try {
            List<OrderAssignment> assignments = orderService.inviteEmployeesToOrder(orderId, employeeIds);
            return ResponseEntity.ok(assignments);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/cart/checkout")
    @PreAuthorize("hasRole('KHACH')")
    @Transactional(rollbackFor = Exception.class) // Nếu 1 đơn lỗi, rollback tất cả
    public ResponseEntity<?> checkoutCart(@RequestBody CartOrderRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        if (request.getServiceIds() == null || request.getServiceIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Danh sách dịch vụ trống"));
        }

        List<Order> createdOrders = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // VÒNG LẶP TÁCH ĐƠN: Với mỗi Service ID -> Tạo 1 Order riêng
        for (Long serviceId : request.getServiceIds()) {
            try {
                Order newOrder = orderService.createOrder(
                        currentUser,
                        serviceId,
                        request.getThoiGianDat(),
                        request.getDiaChiDichVu(),
                        null,
                        request.getPhuongThucThanhToan(),
                        "CHUA_XU_LY",
                        request.getNotes(),
                        request.getSdt());

                createdOrders.add(newOrder);
            } catch (Exception e) {
                errors.add("Lỗi dịch vụ ID " + serviceId + ": " + e.getMessage());
                throw new RuntimeException("Lỗi tạo đơn: " + e.getMessage()); // Throw để kích hoạt rollback
            }
        }

        // Sau khi tạo thành công tất cả -> Xóa giỏ hàng
        cartService.clearCart(currentUser.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Đặt thành công " + createdOrders.size() + " đơn hàng",
                "orderIds", createdOrders.stream().map(Order::getId).collect(Collectors.toList())));
    }
}