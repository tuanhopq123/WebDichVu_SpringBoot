package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.WebDichVu_SpringBoot.entity.Order;
import com.example.WebDichVu_SpringBoot.entity.Review;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByServiceAndUser(Service service, User user);

    Optional<Review> findByOrderAndUser(Order order, User user);

    List<Review> findByServiceOrderByNgayTaoDesc(Service service);

    Page<Review> findByService(Service service, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.service.id = :serviceId")
    Optional<Double> averageRatingByServiceId(@Param("serviceId") Long serviceId); // OK với Double
}