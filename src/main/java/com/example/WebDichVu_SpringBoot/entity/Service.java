package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "SERVICES")
@Data
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_dich_vu", nullable = false, unique = true)
    private String tenDichVu;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "gia_co_ban", nullable = false)
    private BigDecimal giaCoBan;

    @Column(name = "image_url")
    private String imageURL; // Đường dẫn đến ảnh đại diện dịch vụ

    @Column(name = "thoi_gian_hoan_thanh")
    private Integer thoiGianHoanThanh;

    @Column(name = "trang_thai_hien_thi")
    private Boolean trangThaiHienThi = true; // True: Đang hoạt động/Hiển thị

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "category_id", nullable = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Category category;

    // THÊM MỐI QUAN HỆ NGƯỢC ONE-TO-MANY
    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<Order> orders;

    // One-to-Many: Service -> Review (Đánh giá)
    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<Review> reviews;

    @ManyToMany(mappedBy = "dichVuDamNhan", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Set<User> nhanVienThucHien; // Danh sách nhân viên có thể làm dịch vụ này

    @Column(name = "so_nhan_vien_yeu_cau", columnDefinition = "INT DEFAULT 1")
    private Integer soNhanVienYeuCau = 1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenDichVu() {
        return tenDichVu;
    }

    public void setTenDichVu(String tenDichVu) {
        this.tenDichVu = tenDichVu;
    }

    public BigDecimal getGiaCoBan() {
        return giaCoBan;
    }

    public void setGiaCoBan(BigDecimal giaCoBan) {
        this.giaCoBan = giaCoBan;
    }

    public Integer getThoiGianHoanThanh() {
        return thoiGianHoanThanh;
    }

    public void setThoiGianHoanThanh(Integer thoiGianHoanThanh) {
        this.thoiGianHoanThanh = thoiGianHoanThanh;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public Set<User> getNhanVienThucHien() {
        return nhanVienThucHien;
    }

    public void setNhanVienThucHien(Set<User> nhanVienThucHien) {
        this.nhanVienThucHien = nhanVienThucHien;
    }
}
