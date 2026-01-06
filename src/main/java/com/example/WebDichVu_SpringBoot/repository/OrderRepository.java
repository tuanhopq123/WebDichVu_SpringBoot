package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.WebDichVu_SpringBoot.entity.Order;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  @Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.service WHERE o.id = :id")
  Order findByIdWithUserAndService(@Param("id") Long id);

  /**
   * Tìm tất cả đơn hàng của một người dùng, sắp xếp theo ngày tạo mới nhất
   * và JOIN FETCH service để tránh lỗi N+1 query.
   */
  @Query("SELECT o FROM Order o JOIN FETCH o.service WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
  List<Order> findByUserIdWithService(@Param("userId") Long userId);

  @Query("SELECT o FROM Order o " +
      "LEFT JOIN FETCH o.user u " + // Tải user (khách hàng)
      "LEFT JOIN FETCH o.service s " + // Tải dịch vụ
      "LEFT JOIN FETCH o.employee e")
  List<Order> findAllWithUserAndService();

  @Query(value = """
      SELECT
          DATE_FORMAT(o.thoi_gian_dat, :format) as label,
          SUM(o.tong_tien) as total
      FROM orders o
      WHERE o.thoi_gian_dat >= :start AND o.thoi_gian_dat < :end
      GROUP BY label
      ORDER BY label
      """, nativeQuery = true)
  List<Map<String, Object>> getRevenueByPeriod(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("format") String format);

  @Query(value = """
      SELECT
          s.ten_dich_vu as tenDichVu,
          COUNT(*) as soLanDat
      FROM orders o
      JOIN services s ON o.service_id = s.id
      WHERE (:start IS NULL OR o.thoi_gian_dat >= :start)
        AND (:end IS NULL OR o.thoi_gian_dat < :end)
      GROUP BY s.id, s.ten_dich_vu
      ORDER BY soLanDat DESC
      """, // <-- XÓA "LIMIT :limit" KHỎI ĐÂY
      nativeQuery = true)
  List<Map<String, Object>> getTopServices(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      Pageable pageable // <-- THAY THẾ int limit BẰNG Pageable
  );

  @Query("SELECT new map(" +
      "u.id as employeeId, " +
      "u.hoTen as employeeName, " +
      "u.sdt as employeePhone, " +
      "u.email as employeeEmail, " +
      "SUM(o.tongTien * 0.3) as totalUnpaidAmount, " +
      "COUNT(o) as totalOrders) " +
      "FROM Order o JOIN o.employee u " + // Chỉ lấy đơn có gán nhân viên
      "WHERE o.trangThai = :status " +
      "AND o.employeePaymentStatus = :paymentStatus " +
      "GROUP BY u.id, u.hoTen, u.sdt, u.email")
  List<Map<String, Object>> getUnpaidPayrollSummary(
      @Param("status") Order.Status status,
      @Param("paymentStatus") Order.EmployeePaymentStatus paymentStatus);

  // Wrapper (để gọi dễ hơn từ Service)
  default List<Map<String, Object>> getUnpaidPayrollSummary() {
    return getUnpaidPayrollSummary(Order.Status.HOAN_THANH, Order.EmployeePaymentStatus.UNPAID);
  }

  @Query("SELECT o FROM Order o " +
      "JOIN FETCH o.service s " +
      "LEFT JOIN FETCH o.user u " +
      "LEFT JOIN FETCH o.employee e " +
      "WHERE o.employee.id = :employeeId " +
      "AND o.completedAt BETWEEN :start AND :end " +
      "AND o.trangThai = :status " +
      "AND o.employeePaymentStatus = :paymentStatus " +
      "ORDER BY o.completedAt DESC")
  List<Order> findUnpaidOrdersForEmployeeByDateRange(
      @Param("employeeId") Long employeeId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("status") Order.Status status,
      @Param("paymentStatus") Order.EmployeePaymentStatus paymentStatus);

  // HÀM MỚI (Cho Nhân viên)
  List<Order> findByEmployeeIdAndEmployeePaymentStatus(Long employeeId,
      Order.EmployeePaymentStatus paymentStatus);

  @Query("SELECT o FROM Order o " +
      "LEFT JOIN FETCH o.user u " + // Tải user (khách hàng)
      "LEFT JOIN FETCH o.service s " + // Tải dịch vụ
      "LEFT JOIN FETCH o.employee e " + // <-- TẢI NHÂN VIÊN
      "WHERE o.id = :id")
  Optional<Order> findByIdWithDetails(@Param("id") Long id);

  @Query("SELECT o FROM Order o " +
      "JOIN FETCH o.service s " +
      "JOIN FETCH o.employee e " +
      "WHERE o.employeePaymentStatus IN :statuses " + // Sửa thành IN list
      "AND (:search IS NULL OR :search = '' OR " +
      "     LOWER(e.hoTen) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "     LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "     e.sdt LIKE CONCAT('%', :search, '%')) " +
      "AND (cast(:fromDate as date) IS NULL OR o.completedAt >= :fromDate) " +
      "AND (cast(:toDate as date) IS NULL OR o.completedAt <= :toDate) " +
      "ORDER BY o.completedAt DESC")
  List<Order> findPayrollHistory(
      @Param("search") String search,
      @Param("statuses") List<Order.EmployeePaymentStatus> statuses,
      @Param("fromDate") LocalDateTime fromDate,
      @Param("toDate") LocalDateTime toDate);

}