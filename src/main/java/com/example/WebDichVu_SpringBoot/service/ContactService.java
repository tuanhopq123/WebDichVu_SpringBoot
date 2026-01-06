package com.example.WebDichVu_SpringBoot.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.WebDichVu_SpringBoot.entity.Contact;
import com.example.WebDichVu_SpringBoot.repository.ContactRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final EmailService emailService;

    public Contact saveContact(Contact contact) {
        return contactRepository.save(contact);
    }

    public Page<Contact> getContacts(int page, int size, String filter, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (search != null && !search.trim().isEmpty()) {
            String keyword = "%" + search.toLowerCase().trim() + "%";
            return contactRepository.searchContacts(keyword, pageable);
        }

        return switch (filter) {
            case "unread" -> contactRepository.findByIsReadFalse(pageable);
            case "read" -> contactRepository.findByIsReadTrue(pageable);
            case "replied" -> contactRepository.findByAdminReplyIsNotNull(pageable);
            default -> contactRepository.findAll(pageable);
        };
    }

    // Đếm số chưa đọc (dùng cho badge)
    public long countUnread() {
        return contactRepository.countByIsReadFalse();
    }

    // Trả lời + gửi email
    @Transactional
    public void replyAndSendEmail(Long contactId, String reply) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy liên hệ ID: " + contactId));

        contact.setAdminReply(reply);
        contact.setRepliedAt(LocalDateTime.now());
        contact.setRead(true);
        contactRepository.save(contact);

        emailService.sendContactReply(contact, reply);
    }

    // Trong ContactService
    public List<Contact> getAllContactsWithReplyInfo() {
        return contactRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}