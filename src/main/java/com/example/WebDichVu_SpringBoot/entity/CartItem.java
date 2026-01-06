package com.example.WebDichVu_SpringBoot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CART_ITEMS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One: Nhiều chi tiết thuộc về 1 giỏ hàng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Cart cart;

    // Many-to-One: Chi tiết này là của 1 dịch vụ
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Service service;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;
}