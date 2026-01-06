package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.WebDichVu_SpringBoot.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
