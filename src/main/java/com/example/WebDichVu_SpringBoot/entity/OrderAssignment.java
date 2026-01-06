package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ORDER_ASSIGNMENTS")
@Data
@NoArgsConstructor
public class OrderAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // Đơn hàng nào?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee; // Mời nhân viên nào?

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssignmentStatus status = AssignmentStatus.PENDING; // Trạng thái lời mời

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt; // Thời điểm mời

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt; // Thời điểm chấp nhận

    public enum AssignmentStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    @PrePersist
    protected void onAssign() {
        this.assignedAt = LocalDateTime.now();
    }

    // Constructor để tạo lời mời
    public OrderAssignment(Order order, User employee) {
        this.order = order;
        this.employee = employee;
    }
}