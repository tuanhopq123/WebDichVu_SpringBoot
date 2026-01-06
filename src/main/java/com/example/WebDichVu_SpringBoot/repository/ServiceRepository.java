package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.WebDichVu_SpringBoot.entity.Service;

import java.util.Optional;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
  boolean existsByTenDichVu(String tenDichVu);

  Optional<Service> findByTenDichVu(String tenDichVu);

  List<Service> findByCategoryId(Long categoryId);
}