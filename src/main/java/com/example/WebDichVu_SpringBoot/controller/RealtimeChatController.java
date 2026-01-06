package com.example.WebDichVu_SpringBoot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.entity.mongodb.ChatMessage;
import com.example.WebDichVu_SpringBoot.repository.ChatRepository;
import com.example.WebDichVu_SpringBoot.repository.UserRepository;

import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;

import java.security.Principal;
import java.util.*;

@RestController
public class RealtimeChatController {

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @Autowired
  private ChatRepository chatRepository;

  @Autowired
  private MongoTemplate mongoTemplate; // Bổ sung cái này để Group dữ liệu

  @Autowired
  private UserRepository userRepository;

  @MessageMapping("/chat")
  public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
    chatMessage.setTimestamp(new Date());
    chatMessage.setRead(false);

    // TỰ ĐỘNG LẤY USER TỪ TOKEN (Bảo mật & Chính xác)
    if (principal != null) {
      String email = principal.getName();

      // Tìm user trong MySQL bằng email này
      User user = userRepository.findByEmail(email).orElse(null);

      if (user != null) {
        // Tự động điền ID và Tên vào tin nhắn (ghi đè lên cái Client gửi)
        chatMessage.setSenderId(user.getId());
        chatMessage.setSenderName(user.getHoTen());

        if (chatMessage.getRole() == null) {
          chatMessage.setRole("ADMIN".equals(user.getVaiTro()) ? "ADMIN" : "USER");
        }
      }
    }

    if (chatMessage.getSenderName() == null) {
      chatMessage.setSenderName("Khách (Lỗi Auth)");
    }

    ChatMessage saved = chatRepository.save(chatMessage);

    if ("USER".equals(chatMessage.getRole())) {
      // User gửi Admin -> Gửi vào kênh chung
      messagingTemplate.convertAndSend("/topic/admin/messages", saved);
    } else {

      Long recipientId = chatMessage.getRecipientId();
      if (recipientId != null) {
        User recipient = userRepository.findById(recipientId).orElse(null);

        if (recipient != null) {
          // Gửi đến Email (Principal Name)
          messagingTemplate.convertAndSendToUser(
              recipient.getEmail(),
              "/queue/messages",
              saved);
        }
      }
    }
  }

  // API lấy lịch sử chat
  @GetMapping("/api/chat/history/{userId}")
  public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Long userId) {
    // Lấy tất cả tin nhắn trong DB
    List<ChatMessage> allMessages = chatRepository.findAll();

    List<ChatMessage> conversation = new ArrayList<>();
    for (ChatMessage msg : allMessages) {
      if ((msg.getSenderId() != null && msg.getSenderId().equals(userId)) ||
          (msg.getRecipientId() != null && msg.getRecipientId().equals(userId))) {
        conversation.add(msg);
      }
    }

    // Sắp xếp theo thời gian tăng dần (Cũ -> Mới) để hiển thị đúng thứ tự
    conversation.sort(Comparator.comparing(ChatMessage::getTimestamp));

    return ResponseEntity.ok(conversation);
  }

  // Lấy danh sách người nhắn gần đây
  @GetMapping("/api/chat/recent-users")
  public ResponseEntity<List<Map>> getRecentChatUsers() {
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(Criteria.where("role").is("USER")),

        Aggregation.sort(Sort.Direction.DESC, "timestamp"),

        Aggregation.group("senderId")
            .first("senderId").as("userId")
            .first("senderName").as("userName")
            .first("content").as("lastMessage")
            .first("serviceName").as("serviceName")
            .first("timestamp").as("timestamp")
            // --- ĐOẠN MỚI: Đếm số dòng có isRead = false ---
            .sum(ConditionalOperators.when(Criteria.where("isRead").is(false))
                .then(1).otherwise(0))
            .as("unreadCount"),
        // ------------------------------------------------

        Aggregation.project("userId", "userName", "lastMessage", "serviceName", "timestamp", "unreadCount")
            .andExclude("_id"));

    AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "chat_messages", Map.class);
    List<Map> chatUsers = results.getMappedResults();

    List<Map> sortedUsers = new ArrayList<>(chatUsers);
    sortedUsers.sort((a, b) -> {
      Date d1 = (Date) a.get("timestamp");
      Date d2 = (Date) b.get("timestamp");
      if (d1 == null || d2 == null)
        return 0;
      return d2.compareTo(d1);
    });

    return ResponseEntity.ok(sortedUsers);
  }

  @PostMapping("/api/chat/mark-read/{userId}")
  public ResponseEntity<?> markAsRead(@PathVariable Long userId) {
    // Tìm tất cả tin nhắn từ User này gửi đến Admin mà chưa đọc
    List<ChatMessage> unreadMessages = chatRepository.findAll().stream()
        .filter(m -> "USER".equals(m.getRole())
            && m.getSenderId().equals(userId)
            && !m.isRead())
        .toList();

    // Cập nhật trạng thái
    for (ChatMessage msg : unreadMessages) {
      msg.setRead(true);
      chatRepository.save(msg);
    }

    return ResponseEntity.ok().build();
  }

  @GetMapping("/api/chat/my-history")
  public ResponseEntity<List<ChatMessage>> getMyChatHistory(Principal principal) {
    if (principal == null)
      return ResponseEntity.status(401).build();

    String email = principal.getName();
    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null)
      return ResponseEntity.badRequest().build();

    Long userId = user.getId();

    // Lấy tin nhắn mà Sender hoặc Recipient là User này
    List<ChatMessage> history = chatRepository.findAll().stream()
        .filter(msg -> (msg.getSenderId() != null && msg.getSenderId().equals(userId)) ||
            (msg.getRecipientId() != null && msg.getRecipientId().equals(userId)))
        .sorted(Comparator.comparing(ChatMessage::getTimestamp))
        .toList();

    return ResponseEntity.ok(history);
  }

  // User đánh dấu đã đọc tin nhắn từ Admin
  @PostMapping("/api/chat/mark-read-user")
  public ResponseEntity<?> markAsReadByUser(Principal principal) {
    if (principal == null)
      return ResponseEntity.status(401).build();
    String email = principal.getName();
    User user = userRepository.findByEmail(email).orElse(null);

    if (user != null) {
      List<ChatMessage> unread = chatRepository.findAll().stream()
          .filter(m -> "ADMIN".equals(m.getRole())
              && m.getRecipientId() != null
              && m.getRecipientId().equals(user.getId())
              && !m.isRead())
          .toList();

      for (ChatMessage msg : unread) {
        msg.setRead(true);
        chatRepository.save(msg);
      }
    }
    return ResponseEntity.ok().build();
  }

}