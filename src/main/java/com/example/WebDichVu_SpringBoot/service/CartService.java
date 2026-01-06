package com.example.WebDichVu_SpringBoot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.WebDichVu_SpringBoot.entity.*;
import com.example.WebDichVu_SpringBoot.repository.*;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;

    public Cart getCartByUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public void addToCart(String email, Long serviceId) {
        Cart cart = getCartByUser(email);
        com.example.WebDichVu_SpringBoot.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        if (cart.getCartItems() == null) {
            cart.setCartItems(new ArrayList<>());
        }

        // Kiểm tra xem dịch vụ đã có trong giỏ chưa
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getService().getId().equals(serviceId))
                .findFirst();

        if (existingItem.isPresent()) {
            throw new RuntimeException("Dịch vụ này đã có trong giỏ hàng!");
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setService(service);
            newItem.setQuantity(1); // Mặc định 1
            cart.getCartItems().add(newItem);
            cartItemRepository.save(newItem); // Lưu item
        }
    }

    @Transactional
    public void removeCartItem(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional
    public void clearCart(String email) {
        Cart cart = getCartByUser(email);
        cart.getCartItems().clear(); // Xóa list trong object cha để Hibernate tự xử lý orphanRemoval = true
        cartRepository.save(cart);
    }

    @Transactional
    public void updateItemQuantity(Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại trong giỏ"));

        if (quantity <= 0) {
            cartItemRepository.delete(item); // Nếu số lượng <= 0 thì xóa luôn
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }
}