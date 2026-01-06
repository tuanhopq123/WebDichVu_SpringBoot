package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "USERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "mat_khau", nullable = true)
    private String matKhau;

    @Column(name = "sdt", length = 15)
    private String sdt;

    @Column(name = "dia_chi", columnDefinition = "TEXT")
    private String diaChi;

    @Column(name = "avatar_url")
    private String avatarURL;

    // Sử dụng EnumType.STRING để lưu giá trị ENUM dưới dạng chuỗi trong DB
    @Enumerated(EnumType.STRING)
    @Column(name = "vai_tro")
    private Role vaiTro = Role.KHACH;

    public enum TrangThaiLamViec {
        RANH,
        BAN
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_lam_viec")
    private TrangThaiLamViec trangThaiLamViec = TrangThaiLamViec.RANH;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true; // Dùng Boolean thay vì boolean

    @Column(name = "provider")
    private String provider; // null = local, "google" = OAuth

    // One-to-Many: User -> Notification (Thông báo)
    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<Notification> notifications;

    // QUAN HỆ NGƯỢC ONE-TO-MANY HOÀN CHỈNH
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude // NGĂN LỖI KHỞI TẠO JPA
    @ToString.Exclude // NGĂN LỖI Lazy Loading trong log
    @JsonIgnore // NGĂN LỖI ĐỆ QUY JSON (500)
    private List<Order> orders;

    // One-to-Many: User -> Review (Đánh giá)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<Review> reviews;

    // One-to-Many: Nhân viên được giao nhiều đơn hàng
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<Order> donHangDuocGiao; // Các đơn hàng nhân viên này được giao

    // One-to-Many: Nhân viên nhận được nhiều đánh giá
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<Review> danhGiaNhanDuoc; // Các đánh giá nhân viên này nhận được

    // Mối quan hệ N-N: Nhân viên có thể làm nhiều dịch vụ
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "nhanvien_dichvu", joinColumns = @JoinColumn(name = "employee_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Service> dichVuDamNhan; // Các dịch vụ nhân viên này đảm nhận

    public enum Role {
        KHACH,
        ADMIN,
        NHAN_VIEN
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + vaiTro.name()));
    }

    @Override
    public String getPassword() {
        return matKhau != null ? matKhau : "N/A"; // OAuth user không có password
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled != null && isEnabled; // Kiểm tra null
    }

    public void setEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}