package com.example.WebDichVu_SpringBoot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.service.JwtService;
import com.example.WebDichVu_SpringBoot.service.UserService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // TỰ ĐỘNG TẠO USER + SET PROVIDER = GOOGLE
        User user = userService.processOAuthUser(oAuth2User);

        // Tạo JWT từ đối tượng User này
        String jwt = jwtService.generateToken(user);

        // Lấy vai trò (role) từ đối tượng User
        String vaiTro = user.getVaiTro().name(); // Ví dụ: "KHACH", "ADMIN", "NHAN_VIEN"

        String redirectUrl = UriComponentsBuilder.fromUriString("/login.html")
                .queryParam("oauth_token", jwt) // Thêm token
                .queryParam("role", vaiTro) // Thêm vai trò
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}