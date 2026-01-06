package com.example.WebDichVu_SpringBoot.repository;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.WebDichVu_SpringBoot.entity.Cart;
import com.example.WebDichVu_SpringBoot.entity.CartItem;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteByCartId(Long cartId);

    @Query("SELECT c FROM Cart c " +
            "LEFT JOIN FETCH c.cartItems ci " +
            "LEFT JOIN FETCH ci.service " +
            "WHERE c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") Long userId);
}