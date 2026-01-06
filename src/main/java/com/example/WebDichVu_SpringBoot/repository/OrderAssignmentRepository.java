package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.WebDichVu_SpringBoot.entity.OrderAssignment;

import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

       // Tìm các lời mời theo ID nhân viên
       List<OrderAssignment> findByEmployeeId(Long employeeId);

       @Query("SELECT oa FROM OrderAssignment oa " +
                     "JOIN FETCH oa.order o " +
                     "JOIN FETCH o.service s " +
                     "JOIN FETCH o.user u " +
                     "JOIN FETCH oa.employee e " + // <-- THÊM DÒNG NÀY
                     "WHERE oa.employee.id = :employeeId AND oa.status = :status")
       List<OrderAssignment> findByEmployeeIdAndStatus(Long employeeId, OrderAssignment.AssignmentStatus status);

       // Tìm lời mời theo ID đơn hàng
       List<OrderAssignment> findByOrderId(Long orderId);

       @Query("SELECT oa FROM OrderAssignment oa " +
                     "JOIN FETCH oa.order o " +
                     "JOIN FETCH o.service s " +
                     "JOIN FETCH o.user u " +
                     "JOIN FETCH oa.employee e " +
                     "WHERE oa.employee.id = :employeeId " +
                     "ORDER BY oa.assignedAt DESC") // Sắp xếp theo ngày mời mới nhất
       List<OrderAssignment> findHistoryByEmployeeId(Long employeeId);

}