package com.example.WebDichVu_SpringBoot.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.OrderAssignment;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.OrderAssignmentRepository;
import com.example.WebDichVu_SpringBoot.repository.OrderRepository;
import com.example.WebDichVu_SpringBoot.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAssignmentService {

    private final OrderAssignmentRepository assignmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // Lấy các lời mời đang chờ
    public List<OrderAssignment> getPendingAssignments(Long employeeId) {
        User employee = userRepository.findById(employeeId).orElseThrow();

        if (employee.getTrangThaiLamViec() == User.TrangThaiLamViec.BAN) {
            return Collections.emptyList();
        }

        return assignmentRepository.findByEmployeeIdAndStatus(employeeId, OrderAssignment.AssignmentStatus.PENDING);
    }

    @Transactional
    public OrderAssignment acceptAssignment(Long assignmentId, User currentUser) {
        log.info("Nhân viên {} đang chấp nhận assignmentId {}", currentUser.getEmail(), assignmentId);

        User employee = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên"));

        // 1. CHẶN NẾU ĐANG BẬN
        if (employee.getTrangThaiLamViec() == User.TrangThaiLamViec.BAN) {
            throw new IllegalStateException(
                    "Bạn đang trong ca làm việc (BẬN), vui lòng hoàn thành đơn hiện tại trước.");
        }

        OrderAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lời mời."));

        if (!assignment.getEmployee().getId().equals(employee.getId())) {
            throw new SecurityException("Bạn không có quyền chấp nhận lời mời này.");
        }

        if (assignment.getStatus() != OrderAssignment.AssignmentStatus.PENDING) {
            throw new IllegalArgumentException("Lời mời này đã được xử lý hoặc hết hạn.");
        }

        // Cập nhật lời mời
        assignment.setStatus(OrderAssignment.AssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // Cập nhật trạng thái nhân viên -> BẬN
        employee.setTrangThaiLamViec(User.TrangThaiLamViec.BAN);
        userRepository.save(employee);

        Order order = assignment.getOrder();

        // Gán nhân viên vào đơn nếu chưa có
        if (order.getEmployee() == null) {
            order.setEmployee(employee);
            order.setTrangThai(Order.Status.DA_NHAN);
            orderRepository.save(order);
            log.info("Đã gán nhân viên {} cho đơn hàng #{}", employee.getEmail(), order.getId());
        }

        return assignment;
    }

    // API 3: Nhân viên từ chối lời mời
    @Transactional
    public OrderAssignment rejectAssignment(Long assignmentId, User employee) {
        log.info("Nhân viên {} đang từ chối assignmentId {}", employee.getEmail(), assignmentId);

        OrderAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lời mời."));

        if (!assignment.getEmployee().getId().equals(employee.getId())) {
            throw new SecurityException("Bạn không có quyền từ chối lời mời này.");
        }

        if (assignment.getStatus() != OrderAssignment.AssignmentStatus.PENDING) {
            throw new IllegalArgumentException("Lời mời này đã được xử lý.");
        }

        assignment.setStatus(OrderAssignment.AssignmentStatus.REJECTED);
        assignmentRepository.save(assignment);

        return assignment;
    }

    private void checkAndCompleteOrder(Order order) {
        // Tải lại (refresh) order để lấy danh sách assignments mới nhất
        Order refreshedOrder = orderRepository.findById(order.getId()).get();

        int requiredCount = refreshedOrder.getSoLuong(); // Số lượng cần = số lượng khách đặt

        // Đếm số lượng đã chấp nhận
        long acceptedCount = refreshedOrder.getAssignments().stream()
                .filter(a -> a.getStatus() == OrderAssignment.AssignmentStatus.ACCEPTED)
                .count();

        log.info("Đơn hàng {}: Cần {} / Đã nhận {}", refreshedOrder.getId(), requiredCount, acceptedCount);

        // 7. Nếu đủ số lượng
        if (acceptedCount == requiredCount) {
            log.info("Đơn hàng {} đã đủ nhân viên. Chuyển trạng thái sang DA_NHAN.", refreshedOrder.getId());

            // 7a. Cập nhật trạng thái đơn hàng
            refreshedOrder.setTrangThai(Order.Status.DA_NHAN);
            orderRepository.save(refreshedOrder);

            // 7b. Gửi email cho KHÁCH HÀNG
            emailService.sendStatusUpdate(
                    refreshedOrder.getUser(),
                    refreshedOrder,
                    Order.Status.CHUA_XU_LY,
                    Order.Status.DA_NHAN);

            // 7c. (Nâng cao) Tự động từ chối các lời mời PENDING còn lại
            refreshedOrder.getAssignments().stream()
                    .filter(a -> a.getStatus() == OrderAssignment.AssignmentStatus.PENDING)
                    .forEach(pendingAssignment -> {
                        log.info("Tự động từ chối lời mời (ID: {}) cho nhân viên {} vì đơn đã đủ.",
                                pendingAssignment.getId(), pendingAssignment.getEmployee().getEmail());
                        pendingAssignment.setStatus(OrderAssignment.AssignmentStatus.REJECTED);
                        assignmentRepository.save(pendingAssignment);
                    });
        }
    }

    public List<OrderAssignment> getHistoryForEmployee(Long employeeId) {
        return assignmentRepository.findHistoryByEmployeeId(employeeId);
    }

    public List<Order> getPendingPaymentConfirmations(Long employeeId) {
        // Cần tạo hàm này trong OrderRepository
        return orderRepository.findByEmployeeIdAndEmployeePaymentStatus(
                employeeId,
                Order.EmployeePaymentStatus.PENDING_CONFIRMATION);
    }

    @Transactional
    public void confirmPayment(Long orderId, Long employeeId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng."));

        // Kiểm tra bảo mật
        if (order.getEmployee() == null || !order.getEmployee().getId().equals(employeeId)) {
            throw new SecurityException("Bạn không có quyền xác nhận đơn hàng này.");
        }

        // Kiểm tra trạng thái
        if (order.getEmployeePaymentStatus() != Order.EmployeePaymentStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ xác nhận.");
        }

        order.setEmployeePaymentStatus(Order.EmployeePaymentStatus.PAID);
        orderRepository.save(order);
    }
}