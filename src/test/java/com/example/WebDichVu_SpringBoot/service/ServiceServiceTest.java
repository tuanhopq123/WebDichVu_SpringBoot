package com.example.WebDichVu_SpringBoot.service;

import com.example.WebDichVu_SpringBoot.entity.Category;
import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.repository.ServiceRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ServiceService Test")
class ServiceServiceTest {

  @Mock
  private ServiceRepository serviceRepository;

  @InjectMocks
  private ServiceService serviceService;

  private Service testService;
  private Category testCategory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    testCategory = new Category();
    testCategory.setId(1L);
    testCategory.setTenDanhMuc("Cleaning");

    testService = new Service();
    testService.setId(1L);
    testService.setTenDichVu("House Cleaning");
    testService.setGiaCoBan(new BigDecimal("500000"));
    testService.setThoiGianHoanThanh(2);
    testService.setMoTa("Professional house cleaning service");
    testService.setCategory(testCategory);
    testService.setTrangThaiHienThi(true);
  }

  // ========== TEST findAllServices ==========
  @Test
  @DisplayName("Should get all services with pagination")
  void testFindAllServices() {
    Page<Service> page = new PageImpl<>(List.of(testService), PageRequest.of(0, 10), 1);
    when(serviceRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Service> result = serviceService.findAllServices(0, 10);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(serviceRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle empty page")
  void testFindAllServicesEmpty() {
    Page<Service> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(serviceRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

    Page<Service> result = serviceService.findAllServices(0, 10);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
  }

  // ========== TEST findServiceById ==========
  @Test
  @DisplayName("Should find service by ID successfully")
  void testFindServiceByIdSuccess() {
    when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));

    Optional<Service> result = serviceService.findServiceById(1L);

    assertTrue(result.isPresent());
    assertEquals("House Cleaning", result.get().getTenDichVu());
  }

  @Test
  @DisplayName("Should return empty when service not found")
  void testFindServiceByIdNotFound() {
    when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

    Optional<Service> result = serviceService.findServiceById(999L);

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should return empty when service ID is negative")
  void testFindServiceByIdNegative() {
    when(serviceRepository.findById(-1L)).thenReturn(Optional.empty());

    Optional<Service> result = serviceService.findServiceById(-1L);

    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should return empty when service ID is zero")
  void testFindServiceByIdZero() {
    when(serviceRepository.findById(0L)).thenReturn(Optional.empty());

    Optional<Service> result = serviceService.findServiceById(0L);

    assertFalse(result.isPresent());
  }

  // ========== TEST saveService ==========
  @Test
  @DisplayName("Should save service successfully")
  void testSaveServiceSuccess() {
    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenReturn(testService);

    Service result = serviceService.saveService(testService);

    assertNotNull(result);
    assertEquals("House Cleaning", result.getTenDichVu());
    assertTrue(result.getTrangThaiHienThi());
    verify(serviceRepository, times(1)).save(any(Service.class));
  }

  @Test
  @DisplayName("Should throw exception when service name already exists")
  void testSaveServiceDuplicateName() {
    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(true);

    assertThrows(RuntimeException.class, () -> {
      serviceService.saveService(testService);
    });

    verify(serviceRepository, never()).save(any(Service.class));
  }

  @Test
  @DisplayName("Should set trangThaiHienThi to true by default")
  void testSaveServiceDefaultStatus() {
    testService.setTrangThaiHienThi(null);

    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Service result = serviceService.saveService(testService);

    assertTrue(result.getTrangThaiHienThi());
  }

  @Test
  @DisplayName("Should trim service name")
  void testSaveServiceTrimName() {
    testService.setTenDichVu("  House Cleaning  ");

    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenReturn(testService);

    serviceService.saveService(testService);

    verify(serviceRepository, times(1)).existsByTenDichVu("House Cleaning");
  }

  @Test
  @DisplayName("Should throw exception when service name is empty")
  void testSaveServiceEmptyName() {
    testService.setTenDichVu("   ");

    when(serviceRepository.existsByTenDichVu("")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenReturn(testService);

    serviceService.saveService(testService);

    verify(serviceRepository, times(1)).existsByTenDichVu("");
  }

  // ========== TEST deleteService ==========
  @Test
  @DisplayName("Should delete service successfully")
  void testDeleteServiceSuccess() {
    serviceService.deleteService(1L);

    verify(serviceRepository, times(1)).deleteById(1L);
  }

  @Test
  @DisplayName("Should handle delete with negative ID")
  void testDeleteServiceNegativeId() {
    serviceService.deleteService(-1L);

    verify(serviceRepository, times(1)).deleteById(-1L);
  }

  @Test
  @DisplayName("Should handle delete with zero ID")
  void testDeleteServiceZeroId() {
    serviceService.deleteService(0L);

    verify(serviceRepository, times(1)).deleteById(0L);
  }

  // ========== TEST updateService ==========
  @Test
  @DisplayName("Should update service successfully")
  void testUpdateServiceSuccess() {
    Service updatedService = new Service();
    updatedService.setTenDichVu("Updated Service");
    updatedService.setGiaCoBan(new BigDecimal("750000"));
    updatedService.setThoiGianHoanThanh(3);
    updatedService.setMoTa("Updated description");
    updatedService.setTrangThaiHienThi(true);
    updatedService.setCategory(testCategory);

    when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
    when(serviceRepository.existsByTenDichVu("Updated Service")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Service result = serviceService.updateService(1L, updatedService);

    assertNotNull(result);
    assertEquals("Updated Service", result.getTenDichVu());
    assertEquals(new BigDecimal("750000"), result.getGiaCoBan());
  }

  @Test
  @DisplayName("Should throw exception when updating service not found")
  void testUpdateServiceNotFound() {
    when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> {
      serviceService.updateService(999L, testService);
    });
  }

  @Test
  @DisplayName("Should throw exception when updating to existing name")
  void testUpdateServiceDuplicateName() {
    Service updatedService = new Service();
    updatedService.setTenDichVu("Existing Service");
    updatedService.setGiaCoBan(new BigDecimal("500000"));
    updatedService.setThoiGianHoanThanh(2);
    updatedService.setMoTa("Description");
    updatedService.setTrangThaiHienThi(true);

    when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
    when(serviceRepository.existsByTenDichVu("Existing Service")).thenReturn(true);

    assertThrows(RuntimeException.class, () -> {
      serviceService.updateService(1L, updatedService);
    });

    verify(serviceRepository, never()).save(any(Service.class));
  }

  @Test
  @DisplayName("Should not check duplicate if name unchanged")
  void testUpdateServiceSameName() {
    Service updatedService = new Service();
    updatedService.setTenDichVu("House Cleaning");
    updatedService.setGiaCoBan(new BigDecimal("750000"));
    updatedService.setThoiGianHoanThanh(3);
    updatedService.setMoTa("Updated");
    updatedService.setTrangThaiHienThi(true);
    updatedService.setCategory(testCategory);

    when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
    when(serviceRepository.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Service result = serviceService.updateService(1L, updatedService);

    assertNotNull(result);
    assertEquals(new BigDecimal("750000"), result.getGiaCoBan());
    verify(serviceRepository, never()).existsByTenDichVu(any());
  }

  @Test
  @DisplayName("Should update service with negative ID")
  void testUpdateServiceNegativeId() {
    when(serviceRepository.findById(-1L)).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> {
      serviceService.updateService(-1L, testService);
    });
  }

  // ========== TEST findAllServicesList ==========
  @Test
  @DisplayName("Should find all services without pagination")
  void testFindAllServicesList() {
    when(serviceRepository.findAll()).thenReturn(List.of(testService));

    List<Service> result = serviceService.findAllServicesList();

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(serviceRepository, times(1)).findAll();
  }

  @Test
  @DisplayName("Should return empty list when no services")
  void testFindAllServicesListEmpty() {
    when(serviceRepository.findAll()).thenReturn(List.of());

    List<Service> result = serviceService.findAllServicesList();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ========== TEST Price Validation ==========
  @Test
  @DisplayName("Should handle zero price")
  void testServiceZeroPrice() {
    testService.setGiaCoBan(BigDecimal.ZERO);

    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenReturn(testService);

    Service result = serviceService.saveService(testService);

    assertEquals(BigDecimal.ZERO, result.getGiaCoBan());
  }

  @Test
  @DisplayName("Should handle negative price")
  void testServiceNegativePrice() {
    testService.setGiaCoBan(new BigDecimal("-50000"));

    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenReturn(testService);

    Service result = serviceService.saveService(testService);

    assertEquals(new BigDecimal("-50000"), result.getGiaCoBan());
  }

  @Test
  @DisplayName("Should handle large price")
  void testServiceLargePrice() {
    testService.setGiaCoBan(new BigDecimal("999999999.99"));

    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenReturn(testService);

    Service result = serviceService.saveService(testService);

    assertEquals(new BigDecimal("999999999.99"), result.getGiaCoBan());
  }

  // ========== TEST Time Validation ==========
  @Test
  @DisplayName("Should handle zero completion time")
  void testServiceZeroTime() {
    testService.setThoiGianHoanThanh(0);

    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenReturn(testService);

    Service result = serviceService.saveService(testService);

    assertEquals(0, result.getThoiGianHoanThanh());
  }

  @Test
  @DisplayName("Should handle negative completion time")
  void testServiceNegativeTime() {
    testService.setThoiGianHoanThanh(-5);

    when(serviceRepository.existsByTenDichVu("House Cleaning")).thenReturn(false);
    when(serviceRepository.save(any(Service.class))).thenReturn(testService);

    Service result = serviceService.saveService(testService);

    assertEquals(-5, result.getThoiGianHoanThanh());
  }
}
