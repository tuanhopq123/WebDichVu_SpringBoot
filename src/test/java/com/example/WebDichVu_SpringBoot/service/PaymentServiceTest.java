package com.example.WebDichVu_SpringBoot.service;

import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PaymentService Test")
class PaymentServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private PaymentService paymentService;

  private Order testOrder;
  private User testUser;
  private Service testService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("user@example.com");

    testService = new Service();
    testService.setId(100L);
    testService.setTenDichVu("Test Service");
    testService.setGiaCoBan(new BigDecimal("500000"));

    testOrder = new Order();
    testOrder.setId(1L);
    testOrder.setUser(testUser);
    testOrder.setService(testService);
    testOrder.setTongTien(new BigDecimal("500000"));
    testOrder.setPaymentStatus(Order.PaymentStatus.UNPAID);
  }

  // ========== TEST confirmPayment Success ==========
  @Test
  @DisplayName("Should confirm payment successfully")
  void testConfirmPaymentSuccess() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    Order result = paymentService.confirmPayment(1L);

    assertNotNull(result);
    assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
    verify(orderRepository, times(1)).save(any(Order.class));
  }

  // ========== TEST Order Not Found ==========
  @Test
  @DisplayName("Should throw exception when order not found")
  void testConfirmPaymentOrderNotFound() {
    when(orderRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> {
      paymentService.confirmPayment(999L);
    });
  }

  @Test
  @DisplayName("Should throw exception when order ID is negative")
  void testConfirmPaymentNegativeOrderId() {
    when(orderRepository.findById(-1L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> {
      paymentService.confirmPayment(-1L);
    });
  }

  @Test
  @DisplayName("Should throw exception when order ID is zero")
  void testConfirmPaymentZeroOrderId() {
    when(orderRepository.findById(0L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> {
      paymentService.confirmPayment(0L);
    });
  }

  // ========== TEST Already Paid ==========
  @Test
  @DisplayName("Should not change status when already paid")
  void testConfirmPaymentAlreadyPaid() {
    testOrder.setPaymentStatus(Order.PaymentStatus.PAID);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    Order result = paymentService.confirmPayment(1L);

    assertNotNull(result);
    assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
    verify(orderRepository, times(1)).save(any(Order.class));
  }

  // ========== TEST Large Order Amount ==========
  @Test
  @DisplayName("Should handle large payment amounts")
  void testConfirmPaymentLargeAmount() {
    testOrder.setTongTien(new BigDecimal("999999999.99"));

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    Order result = paymentService.confirmPayment(1L);

    assertNotNull(result);
    assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
    assertEquals(new BigDecimal("999999999.99"), result.getTongTien());
  }

  // ========== TEST Zero Amount Order ==========
  @Test
  @DisplayName("Should handle zero amount order")
  void testConfirmPaymentZeroAmount() {
    testOrder.setTongTien(BigDecimal.ZERO);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    Order result = paymentService.confirmPayment(1L);

    assertNotNull(result);
    assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
    assertEquals(BigDecimal.ZERO, result.getTongTien());
  }

  // ========== TEST Negative Amount Order ==========
  @Test
  @DisplayName("Should handle negative amount order (potential refund scenario)")
  void testConfirmPaymentNegativeAmount() {
    testOrder.setTongTien(new BigDecimal("-100000"));

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    Order result = paymentService.confirmPayment(1L);

    assertNotNull(result);
    assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
    assertEquals(new BigDecimal("-100000"), result.getTongTien());
  }

  // ========== TEST Multiple Confirmations ==========
  @Test
  @DisplayName("Should idempotently handle multiple confirmations")
  void testConfirmPaymentMultipleTimes() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    // First confirmation
    Order result1 = paymentService.confirmPayment(1L);
    assertEquals(Order.PaymentStatus.PAID, result1.getPaymentStatus());

    // Second confirmation (should not fail)
    testOrder.setPaymentStatus(Order.PaymentStatus.PAID);
    Order result2 = paymentService.confirmPayment(1L);
    assertEquals(Order.PaymentStatus.PAID, result2.getPaymentStatus());

    verify(orderRepository, times(2)).save(any(Order.class));
  }

  // ========== TEST Null Order Check ==========
  @Test
  @DisplayName("Should handle repository returning null (defensive coding)")
  void testConfirmPaymentRepositoryNull() {
    when(orderRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> {
      paymentService.confirmPayment(1L);
    });
  }

  // ========== TEST Order with Different Statuses ==========
  @Test
  @DisplayName("Should handle order in PENDING status")
  void testConfirmPaymentPendingOrder() {
    testOrder.setTrangThai(Order.Status.CHUA_XU_LY);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    Order result = paymentService.confirmPayment(1L);

    assertNotNull(result);
    assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
    assertEquals(Order.Status.CHUA_XU_LY, result.getTrangThai());
  }

  @Test
  @DisplayName("Should handle order in COMPLETED status")
  void testConfirmPaymentCompletedOrder() {
    testOrder.setTrangThai(Order.Status.HOAN_THANH);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    Order result = paymentService.confirmPayment(1L);

    assertNotNull(result);
    assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
    assertEquals(Order.Status.HOAN_THANH, result.getTrangThai());
  }

  @Test
  @DisplayName("Should handle order in CANCELED status")
  void testConfirmPaymentCanceledOrder() {
    testOrder.setTrangThai(Order.Status.HUY);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

    Order result = paymentService.confirmPayment(1L);

    assertNotNull(result);
    assertEquals(Order.PaymentStatus.PAID, result.getPaymentStatus());
  }
}
