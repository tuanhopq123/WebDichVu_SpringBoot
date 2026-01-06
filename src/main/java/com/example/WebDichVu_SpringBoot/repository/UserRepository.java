package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.WebDichVu_SpringBoot.entity.Service;
import com.example.WebDichVu_SpringBoot.entity.User;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.dichVuDamNhan WHERE u.vaiTro = :role")
    List<User> findUsersByVaiTroWithServices(@Param("role") User.Role role);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.dichVuDamNhan WHERE u.id = :id AND u.vaiTro = :role")
    Optional<User> findByIdAndVaiTroWithServices(@Param("id") Long id, @Param("role") User.Role role);

    List<User> findByVaiTroAndTrangThaiLamViec(User.Role vaiTro, User.TrangThaiLamViec trangThai);

    List<User> findByVaiTroAndTrangThaiLamViecAndDichVuDamNhanContains(
            User.Role vaiTro,
            User.TrangThaiLamViec trangThai,
            Service service);
}