package com.example.WebDichVu_SpringBoot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.example.WebDichVu_SpringBoot.service.JwtService;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    // Chỉ kiểm tra khi Client gửi lệnh CONNECT
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {

      String rawHeader = accessor.getFirstNativeHeader("Authorization");
      log.info("📢 STOMP CONNECT received. Header Authorization: {}", rawHeader);

      // 1. Lấy Token từ Header 'Authorization' của gói tin STOMP
      String authHeader = accessor.getFirstNativeHeader("Authorization");

      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        try {
          // 2. Lấy username từ token
          String userEmail = jwtService.extractUsername(token);

          if (userEmail != null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // 3. Kiểm tra Token hợp lệ
            if (jwtService.isTokenValid(token, userDetails)) {
              // 4. Tạo Authentication object
              UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null,
                  userDetails.getAuthorities());

              // 5. Gắn User vào phiên WebSocket (Quan trọng để Controller biết ai đang gửi)
              accessor.setUser(authToken);

              log.info("WebSocket Authenticated User: {}", userEmail);
            }
          }
        } catch (Exception e) {
          log.error("WebSocket Authentication Failed: {}", e.getMessage());
          // Không làm gì cả, để mặc định là null -> Kết nối sẽ bị từ chối hoặc coi là vô
          // danh
        }
      } else {
        log.warn("WebSocket Connection attempt without Token");
      }
    }
    return message;
  }
}