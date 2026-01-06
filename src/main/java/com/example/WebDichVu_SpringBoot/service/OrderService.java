package com.example.WebDichVu_SpringBoot.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.example.WebDichVu_SpringBoot.entity.Notification;
import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.OrderAssignment;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.OrderAssignmentRepository;
import com.example.WebDichVu_SpringBoot.repository.OrderRepository;
import com.example.WebDichVu_SpringBoot.repository.ReviewRepository;
import com.example.WebDichVu_SpringBoot.repository.ServiceRepository;
import com.example.WebDichVu_SpringBoot.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class OrderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final ReviewRepository reviewRepository;
    private final EmailService emailService;
    private final OrderAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    @Autowired
    public OrderService(OrderRepository orderRepository,
            UserRepository userRepository,
            ServiceRepository serviceRepository,
            ReviewRepository reviewRepository,
            EmailService emailService, OrderAssignmentRepository assignmentRepository,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.reviewRepository = reviewRepository;
        this.emailService = emailService;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
    }

    public Page<Order> findAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAll(pageable);
    }

    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    public Order findOrderById(Long id) {
        return orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng #" + id));
    }

    public List<Order> findAllOrdersForAdmin() {
        return orderRepository.findAllWithUserAndService();
    }

    @Transactional
    public Order createOrder(User currentUser, Long serviceId, String thoiGianDat, String diaChiDichVu,
            BigDecimal tongTien,
            String phuongThucThanhToan, String trangThai, String notes, String sdt) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Không tìm thấy người dùng với ID: " + currentUser.getId()));
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Không tìm thấy dịch vụ với ID: " + serviceId));

        Order order = new Order();
        order.setUser(user);
        order.setService(service);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            order.setThoiGianDat(LocalDateTime.parse(thoiGianDat, formatter));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Thời gian đặt không hợp lệ, yêu cầu định dạng yyyy-MM-dd'T'HH:mm, ví dụ: 2025-10-08T14:30, nhận được: "
                            + thoiGianDat);
        }
        if (diaChiDichVu == null || diaChiDichVu.trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ dịch vụ không được để trống");
        }
        order.setDiaChiDichVu(diaChiDichVu);
        if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tổng tiền phải lớn hơn 0");
        }
        order.setTongTien(tongTien);
        try {
            order.setPhuongThucThanhToan(Order.PaymentMethod.valueOf(phuongThucThanhToan.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không hợp lệ, phải là một trong: TIEN_MAT, CHUYEN_KHOAN, nhận được: "
                            + phuongThucThanhToan);
        }
        try {
            order.setTrangThai(Order.Status.valueOf(trangThai.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ, phải là một trong: CHUA_XU_LY, DA_NHAN, HOAN_THANH, HUY, nhận được: "
                            + trangThai);
        }
        order.setSdt(sdt != null ? sdt.trim() : null);
        order.setNotes(notes != null ? notes.trim() : null);
        // LƯU ĐƠN HÀNG
        Order savedOrder = orderRepository.save(order);

        // GỬI EMAIL XÁC NHẬN ĐẶT LỊCH
        emailService.sendOrderConfirmation(currentUser, savedOrder);

        String tieuDe = "Đặt lịch thành công #" + savedOrder.getId();
        String noiDung = "Dịch vụ: " + savedOrder.getService().getTenDichVu();
        String link = "/bookings_detail.html?booking_id=" + savedOrder.getId();
        notificationService.createNotification(
                currentUser,
                tieuDe,
                noiDung,
                link,
                Notification.Type.ORDER // Dựa trên Notification.java, Type.ORDER là đúng
        );

        return savedOrder;
    }

    @Transactional
    public Order updateOrder(
            Long id,
            Long serviceId,
            String thoiGianDat,
            String diaChiDichVu,
            BigDecimal tongTien,
            String phuongThucThanhToan,
            String trangThai,
            String sdt,
            String paymentStatusStr) {
        Order order = orderRepository.findById(id)
                .orElseThrow(
                        () -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + id));

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Không tìm thấy dịch vụ với ID: " + serviceId));

        // === CẬP NHẬT DỊCH VỤ ===
        order.setService(service);

        // === THỜI GIAN ĐẶT ===
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            order.setThoiGianDat(LocalDateTime.parse(thoiGianDat, formatter));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Thời gian đặt không hợp lệ, yêu cầu định dạng yyyy-MM-dd'T'HH:mm, ví dụ: 2025-10-08T14:30, nhận được: "
                            + thoiGianDat);
        }

        // === ĐỊA CHỈ ===
        if (diaChiDichVu == null || diaChiDichVu.trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ dịch vụ không được để trống");
        }
        order.setDiaChiDichVu(diaChiDichVu);

        // === TỔNG TIỀN ===
        if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tổng tiền phải lớn hơn 0");
        }
        order.setTongTien(tongTien);

        // === PHƯƠNG THỨC THANH TOÁN ===
        try {
            order.setPhuongThucThanhToan(Order.PaymentMethod.valueOf(phuongThucThanhToan.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không hợp lệ, phải là một trong: TIEN_MAT, CHUYEN_KHOAN, nhận được: "
                            + phuongThucThanhToan);
        }

        // === TRẠNG THÁI ĐƠN HÀNG ===
        Order.Status oldStatus = order.getTrangThai();
        Order.Status newStatus;
        try {
            newStatus = Order.Status.valueOf(trangThai.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ, phải là một trong: CHUA_XU_LY, DA_NHAN, HOAN_THANH, HUY, nhận được: "
                            + trangThai);
        }

        // === KIỂM TRA THANH TOÁN TRƯỚC KHI CHO PHÉP HỦY ===
        Order.PaymentStatus currentPaymentStatus = order.getPaymentStatus();
        if (currentPaymentStatus == Order.PaymentStatus.PAID && newStatus == Order.Status.HUY) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đã thanh toán!");
        }

        // **BẮT ĐẦU LOGIC NGHIỆP VỤ MỚI CỦA BẠN**
        boolean statusHasChanged = (oldStatus != newStatus);

        if (statusHasChanged && (newStatus == Order.Status.HOAN_THANH || newStatus == Order.Status.HUY)) {

            // 1. Lưu lại ngày hoàn thành/hủy (để tính lương)
            order.setCompletedAt(LocalDateTime.now());

            // 2. Tự động chuyển trạng thái nhân viên về 'RANH'
            if (order.getEmployee() != null) {
                User employee = order.getEmployee();
                if (newStatus == Order.Status.HOAN_THANH) {
                    order.setEmployeePaymentStatus(Order.EmployeePaymentStatus.UNPAID);
                    log.info("Đơn hàng #{} hoàn thành. Đã kích hoạt trạng thái lương UNPAID cho nhân viên {}",
                            order.getId(), employee.getEmail());
                }
                // Chỉ cập nhật nếu nhân viên đang 'BAN'
                if (employee.getTrangThaiLamViec() == User.TrangThaiLamViec.BAN) {
                    employee.setTrangThaiLamViec(User.TrangThaiLamViec.RANH);
                    userRepository.save(employee);
                    log.info("Đơn hàng #{} chuyển sang {}. Tự động cập nhật nhân viên {} về 'RANH'.",
                            order.getId(), newStatus, employee.getEmail());
                }
            }
        }

        order.setTrangThai(newStatus);

        // === CẬP NHẬT PAYMENT STATUS (CHỈ TỪ UNPAID → PAID) ===
        if (paymentStatusStr != null && !paymentStatusStr.isBlank()) {
            Order.PaymentStatus newPaymentStatus;
            try {
                newPaymentStatus = Order.PaymentStatus.valueOf(paymentStatusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Tình trạng thanh toán không hợp lệ, phải là UNPAID hoặc PAID, nhận được: " + paymentStatusStr);
            }

            // Không cho chuyển từ PAID → UNPAID
            if (currentPaymentStatus == Order.PaymentStatus.PAID && newPaymentStatus == Order.PaymentStatus.UNPAID) {
                throw new IllegalArgumentException("Không thể chuyển từ ĐÃ THANH TOÁN về CHƯA THANH TOÁN!");
            }

            // === GỬI EMAIL KHI CHUYỂN TỪ UNPAID → PAID ===
            if (currentPaymentStatus == Order.PaymentStatus.UNPAID && newPaymentStatus == Order.PaymentStatus.PAID) {
                emailService.sendPaymentSuccess(order.getUser(), order);
                log.info("Gửi email thanh toán thành công cho đơn #{}", order.getId());
                String tieuDe = "Thanh toán thành công #" + order.getId();
                String noiDung = "Đơn hàng của bạn đã được thanh toán.";
                String link = "/payment.html?order_id=" + order.getId();
                notificationService.createNotification(
                        order.getUser(),
                        tieuDe,
                        noiDung,
                        link,
                        Notification.Type.ORDER);
            }

            // === GỬI EMAIL NẾU TRẠNG THÁI THAY ĐỔI ===
            if (oldStatus != newStatus) {
                emailService.sendStatusUpdate(order.getUser(), order, oldStatus, newStatus);
                log.info("Gửi email cập nhật trạng thái từ {} → {} cho đơn #{}", oldStatus, newStatus, order.getId());
                String statusText = getStatusVietnamese(newStatus); // Lấy hàm helper
                String tieuDe = "Đơn hàng #" + order.getId() + " đã " + statusText;
                String noiDung = "Trạng thái đơn hàng của bạn vừa được cập nhật.";
                String link = "/my_bookings.html"; // Link chung
                notificationService.createNotification(
                        order.getUser(),
                        tieuDe,
                        noiDung,
                        link,
                        Notification.Type.ORDER);
            }

            order.setPaymentStatus(newPaymentStatus);
        }

        // === SỐ ĐIỆN THOẠI ===
        order.setSdt(sdt != null ? sdt.trim() : null);

        // === LƯU ===
        return orderRepository.save(order);
    }

    /**
     * Lấy tất cả đơn hàng của người dùng + kiểm tra đã đánh giá chưa
     */
    @Transactional(readOnly = true)
    public List<Order> findOrdersByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new jakarta.persistence.EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId);
        }

        List<Order> orders = orderRepository.findByUserIdWithService(userId);

        // KIỂM TRA TỪNG ĐƠN CÓ ĐÁNH GIÁ CHƯA
        return orders.stream().map(order -> {
            boolean hasReviewed = reviewRepository.findByOrderAndUser(order, order.getUser()).isPresent();
            order.setHasReviewed(hasReviewed);
            return order;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatusString) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Không tìm thấy đơn hàng với ID: " + orderId));
        try {
            Order.Status newStatus = Order.Status.valueOf(newStatusString.toUpperCase());
            order.setTrangThai(newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ, phải là một trong: CHUA_XU_LY, DA_NHAN, HOAN_THANH, HUY, nhận được: "
                            + newStatusString);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(
                        () -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + id));
        orderRepository.delete(order);
    }

    @Transactional
    public void confirmBatchPayment(List<Long> orderIds, BigDecimal paidAmount) {

        // 1. TÌM TẤT CẢ ĐƠN HÀNG VÀ TÍNH TỔNG TIỀN YÊU CẦU
        // Giả sử bạn có findAllById trong OrderRepository
        List<Order> orders = orderRepository.findAllById(orderIds);

        if (orders.size() != orderIds.size()) {
            throw new EntityNotFoundException("Không tìm thấy đủ đơn hàng từ các ID trong nội dung CK.");
        }

        BigDecimal requiredTotalAmount = BigDecimal.ZERO;
        for (Order order : orders) {
            requiredTotalAmount = requiredTotalAmount.add(order.getTongTien());
        }

        // 2. KIỂM TRA TỔNG SỐ TIỀN THANH TOÁN
        if (paidAmount == null || paidAmount.compareTo(requiredTotalAmount) < 0) {
            throw new IllegalArgumentException(
                    String.format("LỖI THANH TOÁN BATCH: Tiền chuyển thiếu. Cần tổng %.0f, đã nhận %.0f.",
                            requiredTotalAmount.doubleValue(), paidAmount != null ? paidAmount.doubleValue() : 0.0));
        }

        // 3. DUYỆT QUA TỪNG ĐƠN HÀNG VÀ CẬP NHẬT TRẠNG THÁI
        for (Order order : orders) {
            if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                // Cập nhật phương thức thanh toán
                if (order.getPhuongThucThanhToan() == Order.PaymentMethod.TIEN_MAT) {
                    order.setPhuongThucThanhToan(Order.PaymentMethod.CHUYEN_KHOAN);
                }
                Order saved = orderRepository.save(order);
                // Gửi email
                emailService.sendPaymentSuccess(saved.getUser(), saved);
            }
        }
    }

    public Map<String, Object> getRevenueByPeriod(String type, String value) { // SỬA 1: Đổi kiểu trả về
        LocalDateTime start, end;
        String format;

        // (Logic tính start, end, format của bạn đã ĐÚNG, giữ nguyên)
        if ("day".equalsIgnoreCase(type) && value != null && !value.isEmpty()) {
            LocalDate date = LocalDate.parse(value);
            start = date.atStartOfDay();
            end = date.plusDays(1).atStartOfDay();
            format = "%H";
        } else if ("month".equalsIgnoreCase(type) && value != null && !value.isEmpty()) {
            YearMonth ym = YearMonth.parse(value);
            start = ym.atDay(1).atStartOfDay();
            end = ym.atEndOfMonth().plusDays(1).atStartOfDay();
            format = "%d";
        } else {
            int year;
            if (value != null && !value.isEmpty()) {
                year = Integer.parseInt(value);
            } else {
                year = LocalDate.now().getYear();
            }
            start = LocalDate.of(year, 1, 1).atStartOfDay();
            end = LocalDate.of(year, 12, 31).plusDays(1).atStartOfDay();
            format = "%m";
        }

        List<Map<String, Object>> revenueData = orderRepository.getRevenueByPeriod(start, end, format);

        double totalRevenue = revenueData.stream()
                .mapToDouble(map -> {
                    Object totalObj = map.get("total");
                    if (totalObj instanceof BigDecimal) {
                        return ((BigDecimal) totalObj).doubleValue();
                    } else if (totalObj instanceof Number) {
                        return ((Number) totalObj).doubleValue();
                    }
                    return 0.0;
                })
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("revenueData", revenueData);
        result.put("totalRevenue", totalRevenue);

        return result;
    }

    public List<Map<String, Object>> getTopServices(String type, String value) {
        LocalDateTime start = null, end = null;

        // SỬA 1: Chỉ xử lý nếu value có nội dung (!value.isEmpty())
        if (value != null && !value.isEmpty()) {
            if ("day".equalsIgnoreCase(type)) {
                LocalDate date = LocalDate.parse(value);
                start = date.atStartOfDay();
                end = date.plusDays(1).atStartOfDay();
            } else if ("month".equalsIgnoreCase(type)) {
                YearMonth ym = YearMonth.parse(value);
                start = ym.atDay(1).atStartOfDay();
                end = ym.atEndOfMonth().plusDays(1).atStartOfDay();
            } else { // 'year'
                // Đã được bảo vệ bởi (value != null && !value.isEmpty())
                int year = Integer.parseInt(value);
                start = LocalDate.of(year, 1, 1).atStartOfDay();
                end = LocalDate.of(year, 12, 31).plusDays(1).atStartOfDay();
            }
        }
        // Nếu value là null hoặc rỗng, start/end sẽ giữ nguyên là null,
        // repository query sẽ tự động bỏ qua điều kiện WHERE (đúng ý đồ)

        // SỬA 2: Thay thế int limit bằng Pageable
        int topLimit = 5; // Bạn muốn lấy top 5
        // PageRequest.of(page, size) -> trang 0 (trang đầu tiên), kích thước là
        // topLimit
        Pageable pageable = PageRequest.of(0, topLimit);

        return orderRepository.getTopServices(start, end, pageable);
    }

    public List<OrderAssignment> inviteEmployeesToOrder(Long orderId, List<Long> employeeIds) {
        // 1. Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        // 2. Kiểm tra trạng thái đơn hàng (chỉ mời khi CHUA_XU_LY)
        if (order.getTrangThai() != Order.Status.CHUA_XU_LY) {
            throw new IllegalArgumentException("Đơn hàng này đã được xử lý hoặc đã hủy.");
        }

        List<OrderAssignment> newAssignments = new ArrayList<>();

        // 3. Lặp qua danh sách ID nhân viên được mời
        for (Long empId : employeeIds) {
            User employee = userRepository.findById(empId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên ID: " + empId));

            // 4. (Tùy chọn) Kiểm tra xem đã mời nhân viên này chưa
            // (Bạn có thể thêm logic kiểm tra trùng lặp ở đây nếu muốn)

            // 5. Tạo đối tượng Phân công (Assignment) mới
            OrderAssignment newAssignment = new OrderAssignment(order, employee);

            // 6. Lưu vào CSDL
            assignmentRepository.save(newAssignment);
            newAssignments.add(newAssignment);

            // 7. Gửi email thông báo cho nhân viên
            // (Bạn cần tạo một template email mới tên là 'assignment-invitation'
            // hoặc dùng email 'simple message' nếu lười)

            // Ví dụ dùng simple message:
            String subject = "Bạn có lời mời làm việc mới cho đơn hàng #" + order.getId();
            String text = "Xin chào " + employee.getHoTen() + ",\n\n" +
                    "Bạn vừa nhận được một lời mời làm dịch vụ: " + order.getService().getTenDichVu() +
                    ".\nXin hãy truy cập trang 'Việc làm của tôi' để chấp nhận hoặc từ chối." +
                    "\n\nCảm ơn.";
            emailService.sendSimpleMessage(employee.getEmail(), subject, text);
        }

        return newAssignments;
    }

    private String getStatusVietnamese(Order.Status status) {
        return switch (status) {
            case CHUA_XU_LY -> "Chờ Xử Lý";
            case DA_NHAN -> "Được Xác Nhận";
            case HOAN_THANH -> "Hoàn Thành";
            case HUY -> "Bị Hủy";
            default -> "Cập Nhật";
        };
    }
}