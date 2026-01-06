package com.example.WebDichVu_SpringBoot.service;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.repository.ServiceRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;

    @Autowired
    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    // Cập nhật để trả về Page thay vì List
    public Page<Service> findAllServices(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return serviceRepository.findAll(pageable);
    }

    // Logic nghiệp vụ: Lấy dịch vụ theo ID
    public Optional<Service> findServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    // Logic nghiệp vụ: Lưu hoặc cập nhật dịch vụ
    public Service saveService(Service service) {
        String tenDichVu = service.getTenDichVu().trim();

        if (serviceRepository.existsByTenDichVu(tenDichVu)) {
            throw new RuntimeException("Tên dịch vụ đã tồn tại!");
        }

        if (service.getTrangThaiHienThi() == null) {
            service.setTrangThaiHienThi(true); // Gán mặc định nếu null
        }
        return serviceRepository.save(service);
    }

    // Logic nghiệp vụ: Xóa dịch vụ
    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    @Transactional
    public Service updateService(Long id, Service service) {
        Service existing = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dịch vụ không tồn tại"));

        String newName = service.getTenDichVu().trim();

        // Chỉ kiểm tra nếu tên thay đổi
        if (!existing.getTenDichVu().equals(newName) &&
                serviceRepository.existsByTenDichVu(newName)) {
            throw new RuntimeException("Tên dịch vụ đã tồn tại!");
        }

        existing.setTenDichVu(newName);
        existing.setGiaCoBan(service.getGiaCoBan());
        existing.setThoiGianHoanThanh(service.getThoiGianHoanThanh());
        existing.setMoTa(service.getMoTa());
        existing.setCategory(service.getCategory());
        existing.setImageURL(service.getImageURL());
        existing.setTrangThaiHienThi(service.getTrangThaiHienThi());

        return serviceRepository.save(existing);
    }

    public List<Service> findAllServicesList() {
        // API này không cần phân trang
        return serviceRepository.findAll();
    }

}