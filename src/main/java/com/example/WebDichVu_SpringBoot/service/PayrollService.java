package com.example.WebDichVu_SpringBoot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.repository.OrderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final OrderRepository orderRepository;

    public List<Map<String, Object>> getPayrollSummary() {
        // Hàm này vẫn giữ nguyên, vì nó chỉ lấy tổng
        return orderRepository.getUnpaidPayrollSummary(
                Order.Status.HOAN_THANH,
                Order.EmployeePaymentStatus.UNPAID);
    }

    public List<Order> getUnpaidOrdersForEmployee(Long employeeId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(23, 59, 59);

        return orderRepository.findUnpaidOrdersForEmployeeByDateRange(
                employeeId,
                start,
                end,
                Order.Status.HOAN_THANH,
                Order.EmployeePaymentStatus.UNPAID);
    }

    @Transactional
    public void requestPaymentForOrders(Long employeeId, LocalDate fromDate, LocalDate toDate) {
        // 1. Lấy danh sách các đơn cần thanh toán
        List<Order> ordersToPay = getUnpaidOrdersForEmployee(employeeId, fromDate, toDate);

        if (ordersToPay.isEmpty()) {
            throw new IllegalArgumentException("Không có đơn hàng nào cần thanh toán trong khoảng thời gian này.");
        }

        // 2. Cập nhật trạng thái
        for (Order order : ordersToPay) {
            // Chuyển sang PENDING_CONFIRMATION (Chờ nhân viên xác nhận đã nhận tiền)
            // Hoặc chuyển thẳng sang PAID nếu bạn muốn trả luôn
            order.setEmployeePaymentStatus(Order.EmployeePaymentStatus.PENDING_CONFIRMATION);
        }

        // Lưu tất cả thay đổi
        orderRepository.saveAll(ordersToPay);
    }

    public List<Order> getHistory(String search, String status, LocalDateTime start, LocalDateTime end) {

        // Xử lý logic lọc trạng thái ngay tại Service
        List<Order.EmployeePaymentStatus> statusList;

        if ("PENDING".equals(status)) {
            statusList = List.of(Order.EmployeePaymentStatus.PENDING_CONFIRMATION);
        } else if ("PAID".equals(status)) {
            statusList = List.of(Order.EmployeePaymentStatus.PAID);
        } else {
            // Mặc định lấy cả 2 (ALL)
            statusList = List.of(Order.EmployeePaymentStatus.PAID, Order.EmployeePaymentStatus.PENDING_CONFIRMATION);
        }

        // Gọi Repository (Lớp Service được phép gọi Repository)
        return orderRepository.findPayrollHistory(search, statusList, start, end);
    }

}