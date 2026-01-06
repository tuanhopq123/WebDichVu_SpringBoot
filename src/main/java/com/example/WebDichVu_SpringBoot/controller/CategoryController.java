package com.example.WebDichVu_SpringBoot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.entity.Category;
import com.example.WebDichVu_SpringBoot.service.CategoryService;

import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@CrossOrigin
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Page<Category>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(categoryService.findAllCategories(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryService.findCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(
            @RequestBody Category category) {
        Category savedCategory = categoryService.saveCategory(category);
        return ResponseEntity.ok(savedCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestBody Category category) {
        return categoryService.findCategoryById(id)
                .map(existingCategory -> {
                    existingCategory.setTenDanhMuc(category.getTenDanhMuc());
                    existingCategory.setMoTa(category.getMoTa());
                    Category updatedCategory = categoryService.saveCategory(existingCategory);
                    return ResponseEntity.ok(updatedCategory);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        if (categoryService.findCategoryById(id).isPresent()) {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Category>> getAllCategoriesForSelect() {
        return ResponseEntity.ok(categoryService.findAllForSelect());
    }
}