package com.example.WebDichVu_SpringBoot.service;

import com.example.WebDichVu_SpringBoot.dto.ReviewRequest;
import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.Review;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.OrderRepository;
import com.example.WebDichVu_SpringBoot.repository.ReviewRepository;
import com.example.WebDichVu_SpringBoot.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ReviewService Test")
class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ServiceRepository serviceRepository;

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private ReviewService reviewService;

  private User testUser;
  private Order testOrder;
  private Service testService;
  private ReviewRequest validRequest;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("user@example.com");

    testService = new Service();
    testService.setId(100L);
    testService.setTenDichVu("Test Service");

    testOrder = new Order();
    testOrder.setId(1L);
    testOrder.setUser(testUser);
    testOrder.setService(testService);
    testOrder.setTrangThai(Order.Status.HOAN_THANH);

    validRequest = new ReviewRequest();
    validRequest.setOrderId(1L);
    validRequest.setServiceId(100L);
    validRequest.setRating(4.5);
    validRequest.setNoiDung("Great service!");
  }

  // ========== TEST createReview Success ==========
  @Test
  @DisplayName("Should create review successfully with valid data")
  void testCreateReviewSuccess() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));
    when(reviewRepository.findByOrderAndUser(testOrder, testUser)).thenReturn(Optional.empty());
    when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Review result = reviewService.createReview(validRequest, testUser);

    assertNotNull(result);
    assertEquals(4.5, result.getRating());
    assertEquals("Great service!", result.getNoiDung());
    verify(reviewRepository, times(1)).save(any(Review.class));
  }

  // ========== TEST Order Not Found ==========
  @Test
  @DisplayName("Should throw exception when order not found")
  void testCreateReviewOrderNotFound() {
    when(orderRepository.findById(999L)).thenReturn(Optional.empty());

    ReviewRequest request = new ReviewRequest();
    request.setOrderId(999L);
    request.setServiceId(100L);

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(request, testUser);
    });
  }

  @Test
  @DisplayName("Should throw exception when order ID is negative")
  void testCreateReviewNegativeOrderId() {
    ReviewRequest request = new ReviewRequest();
    request.setOrderId(-1L);
    request.setServiceId(100L);

    when(orderRepository.findById(-1L)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(request, testUser);
    });
  }

  // ========== TEST Order Not Belong to User ==========
  @Test
  @DisplayName("Should throw exception when order doesn't belong to user")
  void testCreateReviewOrderNotBelongToUser() {
    User otherUser = new User();
    otherUser.setId(2L);
    testOrder.setUser(otherUser);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(validRequest, testUser);
    });
  }

  // ========== TEST Order Status Not HOAN_THANH ==========
  @Test
  @DisplayName("Should throw exception when order status is not HOAN_THANH")
  void testCreateReviewOrderNotCompleted() {
    testOrder.setTrangThai(Order.Status.CHUA_XU_LY);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(validRequest, testUser);
    });
  }

  @Test
  @DisplayName("Should throw exception when order status is CANCELED")
  void testCreateReviewOrderCanceled() {
    testOrder.setTrangThai(Order.Status.HUY);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(validRequest, testUser);
    });
  }

  // ========== TEST Service Not Found ==========
  @Test
  @DisplayName("Should throw exception when service not found")
  void testCreateReviewServiceNotFound() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

    ReviewRequest request = new ReviewRequest();
    request.setOrderId(1L);
    request.setServiceId(999L);

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(request, testUser);
    });
  }

  @Test
  @DisplayName("Should throw exception when service ID is negative")
  void testCreateReviewNegativeServiceId() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(serviceRepository.findById(-1L)).thenReturn(Optional.empty());

    ReviewRequest request = new ReviewRequest();
    request.setOrderId(1L);
    request.setServiceId(-1L);

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(request, testUser);
    });
  }

  // ========== TEST Service Not Match Order ==========
  @Test
  @DisplayName("Should throw exception when order service is null")
  void testCreateReviewOrderServiceNull() {
    testOrder.setService(null);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(validRequest, testUser);
    });
  }

  @Test
  @DisplayName("Should throw exception when service doesn't match order")
  void testCreateReviewServiceNotMatchOrder() {
    Service otherService = new Service();
    otherService.setId(200L);
    testOrder.setService(otherService);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(validRequest, testUser);
    });
  }

  // ========== TEST Rating Validation ==========
  @ParameterizedTest
  @ValueSource(doubles = { -5.0, -1.0, -0.5, 0.0, 0.1, 0.3, 5.1, 6.0, 10.0 })
  @DisplayName("Should throw exception for invalid rating values")
  void testCreateReviewInvalidRating(double invalidRating) {
    validRequest.setRating(invalidRating);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));
    when(reviewRepository.findByOrderAndUser(testOrder, testUser)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(validRequest, testUser);
    });
  }

  @ParameterizedTest
  @ValueSource(doubles = { 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0 })
  @DisplayName("Should accept valid rating values in 0.5 increments")
  void testCreateReviewValidRating(double validRating) {
    validRequest.setRating(validRating);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));
    when(reviewRepository.findByOrderAndUser(testOrder, testUser)).thenReturn(Optional.empty());
    when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Review result = reviewService.createReview(validRequest, testUser);

    assertNotNull(result);
    assertEquals(validRating, result.getRating());
  }

  @Test
  @DisplayName("Should throw exception for null rating")
  void testCreateReviewNullRating() {
    validRequest.setRating(null);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));

    assertThrows(NullPointerException.class, () -> {
      reviewService.createReview(validRequest, testUser);
    });
  }

  // ========== TEST Duplicate Review ==========
  @Test
  @DisplayName("Should throw exception when user already reviewed this order")
  void testCreateReviewDuplicate() {
    Review existingReview = new Review();
    existingReview.setId(1L);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));
    when(reviewRepository.findByOrderAndUser(testOrder, testUser)).thenReturn(Optional.of(existingReview));

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.createReview(validRequest, testUser);
    });
  }

  // ========== TEST getReviewsByService ==========
  @Test
  @DisplayName("Should get reviews by service successfully")
  void testGetReviewsByServiceSuccess() {
    when(serviceRepository.findById(100L)).thenReturn(Optional.of(testService));
    when(reviewRepository.findByServiceOrderByNgayTaoDesc(testService))
        .thenReturn(java.util.List.of());

    var result = reviewService.getReviewsByService(100L);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(serviceRepository, times(1)).findById(100L);
  }

  @Test
  @DisplayName("Should throw exception when service not found in getReviewsByService")
  void testGetReviewsByServiceNotFound() {
    when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.getReviewsByService(999L);
    });
  }

  @Test
  @DisplayName("Should throw exception when service ID is negative in getReviewsByService")
  void testGetReviewsByServiceNegativeId() {
    when(serviceRepository.findById(-1L)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> {
      reviewService.getReviewsByService(-1L);
    });
  }
}
