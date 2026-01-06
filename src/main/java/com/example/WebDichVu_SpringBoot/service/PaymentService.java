package com.example.WebDichVu_SpringBoot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.repository.OrderRepository;

import jakarta.persistence.EntityNotFoundException; // SỬA: jakarta thay vì javax

@Service
public class PaymentService {

    private final OrderRepository orderRepository;

    @Autowired
    public PaymentService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order confirmPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        // Chỉ cập nhật nếu chưa thanh toán
        if (order.getPaymentStatus() == Order.PaymentStatus.UNPAID) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            // Tự động chuyển trạng thái đơn hàng
            // if (order.getTrangThai() == Order.Status.CHUA_XU_LY) {
            // order.setTrangThai(Order.Status.DA_NHAN);
            // }
        }

        return orderRepository.save(order);
    }
}