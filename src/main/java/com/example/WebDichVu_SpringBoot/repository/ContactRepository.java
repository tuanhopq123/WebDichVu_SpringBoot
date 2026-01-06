package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.WebDichVu_SpringBoot.entity.Contact;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    long countByIsReadFalse();

    Page<Contact> findByIsReadFalse(Pageable pageable);

    Page<Contact> findByIsReadTrue(Pageable pageable);

    Page<Contact> findByAdminReplyIsNotNull(Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE " +
            "LOWER(c.name) LIKE LOWER(:keyword) OR " +
            "LOWER(c.email) LIKE LOWER(:keyword) OR " +
            "LOWER(c.message) LIKE LOWER(:keyword)")
    Page<Contact> searchContacts(@Param("keyword") String keyword, Pageable pageable);
}