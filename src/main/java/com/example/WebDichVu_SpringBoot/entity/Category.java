package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Entity
@Table(name = "CATEGORIES")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_danh_muc", nullable = false, unique = true)
    private String tenDanhMuc;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Service> services;
}