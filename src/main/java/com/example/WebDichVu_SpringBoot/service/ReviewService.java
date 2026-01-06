package com.example.WebDichVu_SpringBoot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.WebDichVu_SpringBoot.dto.ReviewDTO;
import com.example.WebDichVu_SpringBoot.dto.ReviewRequest;
import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.Review;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.repository.OrderRepository;
import com.example.WebDichVu_SpringBoot.repository.ReviewRepository;
import com.example.WebDichVu_SpringBoot.repository.ServiceRepository;

import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private OrderRepository orderRepository;

    public Review createReview(ReviewRequest request, User currentUser) {
        // Fetch order từ orderId
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));

        // Kiểm tra order thuộc user hiện tại
        if (!order.getUser().equals(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đánh giá đơn hàng này");
        }

        // Kiểm tra trạng thái order (chỉ review khi HOAN_THANH)
        if (order.getTrangThai() != Order.Status.HOAN_THANH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể đánh giá đơn hàng đã hoàn thành");
        }

        // Fetch service từ request
        com.example.WebDichVu_SpringBoot.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dịch vụ không tồn tại"));

        // Kiểm tra service khớp với order (fix NPE: check null)
        com.example.WebDichVu_SpringBoot.entity.Service orderService = order.getService();
        if (orderService == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng không có dịch vụ liên kết");
        }
        if (!service.getId().equals(orderService.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dịch vụ không khớp với đơn hàng");
        }

        // Kiểm tra đã đánh giá chưa (theo order + user)
        if (reviewRepository.findByOrderAndUser(order, currentUser).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã đánh giá đơn hàng này rồi");
        }

        // Validate rating (SỬA: Double từ 0.5 đến 5.0, check multiple 0.5)
        Double rating = request.getRating();
        if (rating < 0.5 || rating > 5.0 || (rating % 0.5 != 0)) { // SỬA: Cho phép 0.5-5.0, bước 0.5
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Điểm đánh giá phải từ 0.5 đến 5.0 (bước 0.5)");
        }

        // Tạo review mới
        Review review = new Review();
        review.setOrder(order);
        review.setService(service);
        review.setUser(currentUser);
        review.setRating(rating); // SỬA: Double
        review.setNoiDung(request.getNoiDung());
        // ngayTao sẽ tự set = LocalDateTime.now() trong entity

        return reviewRepository.save(review);
    }

    // Các method khác giữ nguyên
    public List<Review> getReviewsByService(Long serviceId) {
        com.example.WebDichVu_SpringBoot.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dịch vụ không tồn tại"));
        return reviewRepository.findByServiceOrderByNgayTaoDesc(service);
    }

    public List<ReviewDTO> getReviewDTOsByService(Long serviceId) {
        List<Review> reviews = getReviewsByService(serviceId); // Gọi hàm ở trên

        return reviews.stream()
                .map(ReviewDTO::new) // Tương đương .map(review -> new ReviewDTO(review))
                .collect(Collectors.toList());
    }

    public Page<Review> getReviewsByServicePaged(Long serviceId, Pageable pageable) {
        com.example.WebDichVu_SpringBoot.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dịch vụ không tồn tại"));
        return reviewRepository.findByService(service, pageable);
    }

    public Optional<Double> getAverageRating(Long serviceId) {
        return reviewRepository.averageRatingByServiceId(serviceId); // SỬA: Đổi tên method nếu cần, nhưng AVG Double OK
    }
}