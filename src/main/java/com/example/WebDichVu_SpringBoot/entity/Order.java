package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.ToString;
import java.util.List;

@Entity
@Table(name = "ORDERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "orders", "assignments", "password",
            "hibernateLazyInitializer", "handler" })
    private User user;

    // Mối quan hệ N-1: Đơn hàng này được gán cho nhân viên nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "orders", "assignments", "password",
            "hibernateLazyInitializer", "handler" })
    private User employee; // Đây là nhân viên thực hiện đơn

    // Trường này có trong SQL của bạn nhưng thiếu trong Entity
    @Column(name = "so_luong", nullable = false, columnDefinition = "int DEFAULT 1")
    private int soLuong = 1; // Mặc định là 1

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "orders", "hibernateLazyInitializer", "handler" })
    private Service service;

    @Column(name = "thoi_gian_dat", nullable = false)
    private LocalDateTime thoiGianDat;

    @Column(name = "dia_chi_dich_vu", columnDefinition = "TEXT", nullable = false)
    private String diaChiDichVu;

    @Column(name = "tong_tien", nullable = false)
    private BigDecimal tongTien;

    @Enumerated(EnumType.STRING)
    @Column(name = "phuong_thuc_thanh_toan", nullable = false)
    private PaymentMethod phuongThucThanhToan;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private Status trangThai = Status.CHUA_XU_LY;

    public enum Status {
        CHUA_XU_LY,
        DA_NHAN,
        HOAN_THANH,
        HUY
    }

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<OrderAssignment> assignments;

    public enum PaymentMethod {
        TIEN_MAT, CHUYEN_KHOAN
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Column(name = "sdt", length = 20)
    private String sdt;

    @Column(name = "notes")
    private String notes;

    public enum PaymentStatus {
        UNPAID, PAID
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Transient
    private Boolean hasReviewed = false;

    public Boolean getHasReviewed() {
        return hasReviewed != null ? hasReviewed : false;
    }

    public void setHasReviewed(Boolean hasReviewed) {
        this.hasReviewed = hasReviewed != null ? hasReviewed : false;
    }

    // Trạng thái thanh toán LƯƠNG cho nhân viên
    public enum EmployeePaymentStatus {
        UNPAID,
        PENDING_CONFIRMATION, // Chưa thanh toán
        PAID // Đã thanh toán
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_payment_status")
    private EmployeePaymentStatus employeePaymentStatus = EmployeePaymentStatus.UNPAID;

    // === THÊM TRƯỜNG MỚI ===
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public EmployeePaymentStatus getEmployeePaymentStatus() {
        return employeePaymentStatus;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setEmployeePaymentStatus(EmployeePaymentStatus employeePaymentStatus) {
        this.employeePaymentStatus = employeePaymentStatus;
    }

    public User getUser() {
        return user;
    }

    public User getEmployee() {
        return employee;
    }

    public void setEmployee(User employee) {
        this.employee = employee;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

}