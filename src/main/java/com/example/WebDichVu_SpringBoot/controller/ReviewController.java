package com.example.WebDichVu_SpringBoot.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import com.example.WebDichVu_SpringBoot.dto.ReviewDTO;
import com.example.WebDichVu_SpringBoot.dto.ReviewRequest;
import com.example.WebDichVu_SpringBoot.entity.Review;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.service.JwtService;
import com.example.WebDichVu_SpringBoot.service.ReviewService;
import com.example.WebDichVu_SpringBoot.service.UserService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<Review> createReview(@Valid @RequestBody ReviewRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7); // Remove "Bearer "
        String username = jwtService.extractUsername(jwt);
        User currentUser = userService.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Review review = reviewService.createReview(request, currentUser);
        return ResponseEntity.ok(review);
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByService(@PathVariable Long serviceId) {
        // Sửa: gọi hàm mới getReviewDTOsByService
        List<ReviewDTO> reviewDTOs = reviewService.getReviewDTOsByService(serviceId);
        return ResponseEntity.ok(reviewDTOs);
    }

    @GetMapping("/service/{serviceId}/paged")
    public ResponseEntity<Page<Review>> getReviewsByServicePaged(@PathVariable Long serviceId,
            Pageable pageable) {
        Page<Review> reviews = reviewService.getReviewsByServicePaged(serviceId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/service/{serviceId}/average-rating")
    public ResponseEntity<Optional<Double>> getAverageRating(@PathVariable Long serviceId) {
        Optional<Double> averageRating = reviewService.getAverageRating(serviceId); // SỬA: Đổi 'averageRating' thành
                                                                                    // 'getAverageRating'
        return ResponseEntity.ok(averageRating);
    }
}