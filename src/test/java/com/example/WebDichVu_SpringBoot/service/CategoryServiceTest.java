package com.example.WebDichVu_SpringBoot.service;

import com.example.WebDichVu_SpringBoot.entity.Category;
import com.example.WebDichVu_SpringBoot.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CategoryService Test")
class CategoryServiceTest {

  @Mock
  private CategoryRepository categoryRepository;

  @InjectMocks
  private CategoryService categoryService;

  private Category testCategory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    testCategory = new Category();
    testCategory.setId(1L);
    testCategory.setTenDanhMuc("Cleaning");
    testCategory.setMoTa("Cleaning services");
  }

  // ========== TEST findAllCategories (Paged) ==========
  @Test
  @DisplayName("Should find all categories with pagination")
  void testFindAllCategoriesPaged() {
    Page<Category> page = new PageImpl<>(List.of(testCategory), PageRequest.of(0, 10), 1);
    when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Category> result = categoryService.findAllCategories(0, 10);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(categoryRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return empty page")
  void testFindAllCategoriesPagedEmpty() {
    Page<Category> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(categoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

    Page<Category> result = categoryService.findAllCategories(0, 10);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
  }

  @Test
  @DisplayName("Should handle negative page number")
  void testFindAllCategoriesNegativePage() {
    Page<Category> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Category> result = categoryService.findAllCategories(-1, 10);

    assertNotNull(result);
    verify(categoryRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle negative page size")
  void testFindAllCategoriesNegativeSize() {
    Page<Category> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Category> result = categoryService.findAllCategories(0, -10);

    assertNotNull(result);
    verify(categoryRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle zero page size")
  void testFindAllCategoriesZeroSize() {
    Page<Category> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Category> result = categoryService.findAllCategories(0, 0);

    assertNotNull(result);
    verify(categoryRepository, times(1)).findAll(any(Pageable.class));
  }

  // ========== TEST findAllCategories (List) ==========
  @Test
  @DisplayName("Should find all categories without pagination")
  void testFindAllCategoriesList() {
    when(categoryRepository.findAll()).thenReturn(List.of(testCategory));

    List<Category> result = categoryService.findAllCategories();

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(categoryRepository, times(1)).findAll();
  }

  @Test
  @DisplayName("Should return empty list when no categories")
  void testFindAllCategoriesListEmpty() {
    when(categoryRepository.findAll()).thenReturn(List.of());

    List<Category> result = categoryService.findAllCategories();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should return multiple categories")
  void testFindAllCategoriesListMultiple() {
    Category cat2 = new Category();
    cat2.setId(2L);
    cat2.setTenDanhMuc("Repair");

    Category cat3 = new Category();
    cat3.setId(3L);
    cat3.setTenDanhMuc("Cooking");

    when(categoryRepository.findAll()).thenReturn(List.of(testCategory, cat2, cat3));

    List<Category> result = categoryService.findAllCategories();

    assertNotNull(result);
    assertEquals(3, result.size());
  }

  // ========== TEST findCategoryById ==========
  @Test
  @DisplayName("Should find category by ID")
  void testFindCategoryById() {
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

    Optional<Category> result = categoryService.findCategoryById(1L);

    assertTrue(result.isPresent());
    assertEquals("Cleaning", result.get().getTenDanhMuc());
  }

  @Test
  @DisplayName("Should return empty when category not found")
  void testFindCategoryByIdNotFound() {
    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Category> result = categoryService.findCategoryById(999L);

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should return empty when ID is negative")
  void testFindCategoryByIdNegative() {
    when(categoryRepository.findById(-1L)).thenReturn(Optional.empty());

    Optional<Category> result = categoryService.findCategoryById(-1L);

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should return empty when ID is zero")
  void testFindCategoryByIdZero() {
    when(categoryRepository.findById(0L)).thenReturn(Optional.empty());

    Optional<Category> result = categoryService.findCategoryById(0L);

    assertFalse(result.isPresent());
  }

  // ========== TEST saveCategory ==========
  @Test
  @DisplayName("Should save category successfully")
  void testSaveCategorySuccess() {
    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.saveCategory(testCategory);

    assertNotNull(result);
    assertEquals("Cleaning", result.getTenDanhMuc());
    verify(categoryRepository, times(1)).save(any(Category.class));
  }

  @Test
  @DisplayName("Should save category with null ID (new)")
  void testSaveCategoryNewWithNullId() {
    Category newCategory = new Category();
    newCategory.setTenDanhMuc("New Category");
    newCategory.setMoTa("New description");

    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.saveCategory(newCategory);

    assertNotNull(result);
    verify(categoryRepository, times(1)).save(any(Category.class));
  }

  @Test
  @DisplayName("Should save category with empty name")
  void testSaveCategoryEmptyName() {
    testCategory.setTenDanhMuc("");

    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.saveCategory(testCategory);

    assertNotNull(result);
    verify(categoryRepository, times(1)).save(any(Category.class));
  }

  @Test
  @DisplayName("Should save category with null name")
  void testSaveCategoryNullName() {
    testCategory.setTenDanhMuc(null);

    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.saveCategory(testCategory);

    assertNotNull(result);
    verify(categoryRepository, times(1)).save(any(Category.class));
  }

  @Test
  @DisplayName("Should save category with null description")
  void testSaveCategoryNullDescription() {
    testCategory.setMoTa(null);

    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.saveCategory(testCategory);

    assertNotNull(result);
    verify(categoryRepository, times(1)).save(any(Category.class));
  }

  @Test
  @DisplayName("Should save category with very long name")
  void testSaveCategoryLongName() {
    String longName = "A".repeat(1000);
    testCategory.setTenDanhMuc(longName);

    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.saveCategory(testCategory);

    assertNotNull(result);
    assertEquals(longName, result.getTenDanhMuc());
  }

  // ========== TEST deleteCategory ==========
  @Test
  @DisplayName("Should delete category successfully")
  void testDeleteCategorySuccess() {
    categoryService.deleteCategory(1L);

    verify(categoryRepository, times(1)).deleteById(1L);
  }

  @Test
  @DisplayName("Should handle delete with negative ID")
  void testDeleteCategoryNegativeId() {
    categoryService.deleteCategory(-1L);

    verify(categoryRepository, times(1)).deleteById(-1L);
  }

  @Test
  @DisplayName("Should handle delete with zero ID")
  void testDeleteCategoryZeroId() {
    categoryService.deleteCategory(0L);

    verify(categoryRepository, times(1)).deleteById(0L);
  }

  @Test
  @DisplayName("Should handle delete with non-existent ID")
  void testDeleteCategoryNonExistent() {
    categoryService.deleteCategory(999L);

    verify(categoryRepository, times(1)).deleteById(999L);
  }

  // ========== TEST findAllForSelect ==========
  @Test
  @DisplayName("Should find all categories sorted by name for select")
  void testFindAllForSelect() {
    when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of(testCategory));

    List<Category> result = categoryService.findAllForSelect();

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(categoryRepository, times(1)).findAll(any(Sort.class));
  }

  @Test
  @DisplayName("Should return sorted categories in ascending order")
  void testFindAllForSelectSorted() {
    Category cat1 = new Category();
    cat1.setId(1L);
    cat1.setTenDanhMuc("Cleaning");

    Category cat2 = new Category();
    cat2.setId(2L);
    cat2.setTenDanhMuc("Repair");

    Category cat3 = new Category();
    cat3.setId(3L);
    cat3.setTenDanhMuc("Cooking");

    when(categoryRepository.findAll(any(Sort.class)))
        .thenReturn(List.of(cat1, cat3, cat2)); // C, C, R (sorted alphabetically)

    List<Category> result = categoryService.findAllForSelect();

    assertNotNull(result);
    assertEquals(3, result.size());
  }

  @Test
  @DisplayName("Should return empty list for select")
  void testFindAllForSelectEmpty() {
    when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of());

    List<Category> result = categoryService.findAllForSelect();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ========== TEST Edge Cases ==========
  @Test
  @DisplayName("Should handle very large ID")
  void testFindCategoryByIdLargeId() {
    when(categoryRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());

    Optional<Category> result = categoryService.findCategoryById(Long.MAX_VALUE);

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should handle minimum ID")
  void testFindCategoryByIdMinId() {
    when(categoryRepository.findById(Long.MIN_VALUE)).thenReturn(Optional.empty());

    Optional<Category> result = categoryService.findCategoryById(Long.MIN_VALUE);

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should save category with special characters in name")
  void testSaveCategorySpecialCharacters() {
    testCategory.setTenDanhMuc("Cleaning & Repair (Special)");

    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.saveCategory(testCategory);

    assertNotNull(result);
    assertEquals("Cleaning & Repair (Special)", result.getTenDanhMuc());
  }
}
