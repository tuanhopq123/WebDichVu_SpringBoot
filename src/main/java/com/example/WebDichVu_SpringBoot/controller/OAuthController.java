package com.example.WebDichVu_SpringBoot.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.WebDichVu_SpringBoot.dto.AuthenticationResponse;
import com.example.WebDichVu_SpringBoot.entity.User;
import com.example.WebDichVu_SpringBoot.service.JwtService;
import com.example.WebDichVu_SpringBoot.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class OAuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public OAuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/oauth-success")
    public void handleOAuthSuccess(Authentication authentication, HttpServletResponse response) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        User user = userService.processOAuthUser(oAuth2User);

        // Tạo JWT
        String jwt = jwtService.generateToken(user);

        // Redirect về home với token (hoặc set cookie)
        response.sendRedirect("/home.html?token=" + jwt + "&email=" + user.getEmail());
    }
}