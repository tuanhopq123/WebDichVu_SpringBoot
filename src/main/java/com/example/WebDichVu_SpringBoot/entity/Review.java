package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "REVIEWS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user; // Người dùng đánh giá

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Service service; // Dịch vụ được đánh giá

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id") // Map với cột 'employee_id' trong DB
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User employee; // Nhân viên thực hiện đơn hàng (bị đánh giá)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order; // Đơn hàng được đánh giá

    @Column(name = "danh_gia_sao", nullable = false) // SỬA: Double với scale 1 (e.g., 1.5)
    private Double rating; // SỬA: Đổi Integer → Double để hỗ trợ half-star (0.5-5.0)

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public User getEmployee() {
        return employee;
    }

    public void setEmployee(User employee) {
        this.employee = employee;
    }
}