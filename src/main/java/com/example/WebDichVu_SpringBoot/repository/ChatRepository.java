package com.example.WebDichVu_SpringBoot.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.WebDichVu_SpringBoot.entity.mongodb.ChatMessage;

import java.util.List;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {
  // Lấy tin nhắn giữa 2 người (hoặc User với Admin) theo ngữ cảnh dịch vụ
  List<ChatMessage> findBySenderIdAndRecipientId(Long senderId, Long recipientId);

  // API cho Admin: Lấy danh sách tin nhắn mới nhất để hiển thị Inbox
  // (Logic này có thể xử lý kỹ hơn ở Service để group by User)
}