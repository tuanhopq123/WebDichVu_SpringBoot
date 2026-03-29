package com.example.WebDichVu_SpringBoot.service;

import com.example.WebDichVu_SpringBoot.entity.Cart;
import com.example.WebDichVu_SpringBoot.entity.CartItem;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.CartItemRepository;
import com.example.WebDichVu_SpringBoot.repository.CartRepository;
import com.example.WebDichVu_SpringBoot.repository.ServiceRepository;
import com.example.WebDichVu_SpringBoot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CartService Test")
class CartServiceTest {

  @Mock
  private CartRepository cartRepository;

  @Mock
  private CartItemRepository cartItemRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ServiceRepository serviceRepository;

  @InjectMocks
  private CartService cartService;

  private User testUser;
  private Cart testCart;
  private Service testService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@example.com");

    testCart = new Cart();
    testCart.setId(1L);
    testCart.setUser(testUser);
    testCart.setCartItems(new ArrayList<>());

    testService = new Service();
    testService.setId(100L);
    testService.setTenDichVu("Test Service");
  }

  // ========== TEST getCartByUser ==========
  @Test
  @DisplayName("Should get existing cart by user email")
  void testGetCartByUserExisting() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

    Cart result = cartService.getCartByUser("test@example.com");

    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(userRepository, times(1)).findByEmail("test@example.com");
  }

  @Test
  @DisplayName("Should create new cart if not exists")
  void testGetCartByUserCreateNew() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
    when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

    Cart result = cartService.getCartByUser("test@example.com");

    assertNotNull(result);
    verify(cartRepository, times(1)).save(any(Cart.class));
  }

  @Test
  @DisplayName("Should throw RuntimeException when user not found")
  void testGetCartByUserNotFound() {
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> {
      cartService.getCartByUser("nonexistent@example.com");
    });
  }

  // ========== TEST addToCart ==========
  @Test
  @DisplayName("Should add new service to cart successfully")
  void testAddToCartSuccess() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));
    when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    cartService.addToCart("test@example.com", 100L);

    assertEquals(1, testCart.getCartItems().size());
    verify(cartItemRepository, times(1)).save(any(CartItem.class));
  }

  @Test
  @DisplayName("Should throw error when service already in cart")
  void testAddToCartDuplicate() {
    CartItem existingItem = new CartItem();
    existingItem.setService(testService);
    testCart.getCartItems().add(existingItem);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));

    assertThrows(RuntimeException.class, () -> {
      cartService.addToCart("test@example.com", 100L);
    });
  }

  @Test
  @DisplayName("Should throw error when service not found")
  void testAddToCartServiceNotFound() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
    when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> {
      cartService.addToCart("test@example.com", 999L);
    });
  }

  @Test
  @DisplayName("Should throw error when negative service ID")
  void testAddToCartNegativeServiceId() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
    when(serviceRepository.findById(-1L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> {
      cartService.addToCart("test@example.com", -1L);
    });
  }

  // ========== TEST removeCartItem ==========
  @Test
  @DisplayName("Should remove cart item successfully")
  void testRemoveCartItem() {
    cartService.removeCartItem(1L);
    verify(cartItemRepository, times(1)).deleteById(1L);
  }

  @Test
  @DisplayName("Should handle removing non-existent cart item")
  void testRemoveCartItemNotFound() {
    doNothing().when(cartItemRepository).deleteById(-1L);
    cartService.removeCartItem(-1L);
    verify(cartItemRepository, times(1)).deleteById(-1L);
  }

  // ========== TEST clearCart ==========
  @Test
  @DisplayName("Should clear all items from cart")
  void testClearCart() {
    CartItem item1 = new CartItem();
    CartItem item2 = new CartItem();
    testCart.getCartItems().addAll(java.util.List.of(item1, item2));

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

    cartService.clearCart("test@example.com");

    assertTrue(testCart.getCartItems().isEmpty());
    verify(cartRepository, times(1)).save(testCart);
  }

  // ========== TEST updateItemQuantity ==========
  @Test
  @DisplayName("Should update item quantity successfully")
  void testUpdateItemQuantityValid() {
    CartItem item = new CartItem();
    item.setId(1L);
    item.setQuantity(1);

    when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

    cartService.updateItemQuantity(1L, 5);

    assertEquals(5, item.getQuantity());
    verify(cartItemRepository, times(1)).save(item);
  }

  @Test
  @DisplayName("Should delete item when quantity is 0")
  void testUpdateItemQuantityZero() {
    CartItem item = new CartItem();
    item.setId(1L);
    item.setQuantity(1);

    when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

    cartService.updateItemQuantity(1L, 0);

    verify(cartItemRepository, times(1)).delete(item);
  }

  @Test
  @DisplayName("Should delete item when quantity is negative")
  void testUpdateItemQuantityNegative() {
    CartItem item = new CartItem();
    item.setId(1L);
    item.setQuantity(1);

    when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

    cartService.updateItemQuantity(1L, -5);

    verify(cartItemRepository, times(1)).delete(item);
  }

  @Test
  @DisplayName("Should throw error when cart item not found")
  void testUpdateItemQuantityNotFound() {
    when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> {
      cartService.updateItemQuantity(999L, 5);
    });
  }
}
