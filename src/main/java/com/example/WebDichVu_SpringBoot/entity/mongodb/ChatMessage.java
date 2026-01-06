package com.example.WebDichVu_SpringBoot.entity.mongodb;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "chat_messages")
public class ChatMessage {
  @Id
  private String id;
  private Long senderId;
  private String senderName;
  private Long recipientId;
  private String recipientName;

  private String content;
  private Date timestamp;
  private boolean isRead; // Trạng thái đã xem

  private Long serviceId; // ID dịch vụ khách đang xem
  private String serviceName; // Tên dịch vụ

  private String role; // "USER" hoặc "ADMIN"
}