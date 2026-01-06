package com.example.WebDichVu_SpringBoot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.entity.Cart;
import com.example.WebDichVu_SpringBoot.service.CartService;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getMyCart() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(cartService.getCartByUser(email));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Long> payload) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            cartService.addToCart(email, payload.get("serviceId"));
            return ResponseEntity.ok(Map.of("message", "Đã thêm vào giỏ hàng"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> removeItem(@PathVariable Long id) {
        cartService.removeCartItem(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa"));
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.clearCart(email);
        return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ giỏ hàng"));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<?> updateItemQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        try {
            Integer quantity = payload.get("quantity");
            if (quantity == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu thông tin số lượng"));
            }
            cartService.updateItemQuantity(id, quantity);
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật số lượng"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}