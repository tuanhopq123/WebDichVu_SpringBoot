package com.example.WebDichVu_SpringBoot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.WebDichVu_SpringBoot.service.JwtService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        System.out
                .println("JWT Filter: Request URI = " + request.getRequestURI() + ", Method = " + request.getMethod());

        // 1. Kiểm tra Header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("JWT Filter: No Bearer token found, continuing chain");
            filterChain.doFilter(request, response);
            return;
        }

        // Lấy JWT Token
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt); // Lấy username/email từ Token

        System.out.println("JWT Filter: Extracted userEmail = " + userEmail + " from token");

        // 2. Kiểm tra nếu tìm thấy email VÀ chưa có xác thực trong SecurityContext
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                // Tải thông tin người dùng từ DB
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                System.out.println("JWT Filter: USER LOADED: " + userDetails.getUsername() +
                        " | Roles: " + userDetails.getAuthorities());

                // 3. Kiểm tra tính hợp lệ của Token
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // =======================================================
                    // === BẮT ĐẦU SỬA ĐỔI ===
                    // 4. KIỂM TRA USER CÓ BỊ KHÓA KHÔNG
                    if (userDetails.isEnabled()) {
                        // (Đây là code cũ của bạn)
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        System.out.println("JWT Filter: Authentication set for user: " + userEmail);

                    } else {
                        // NẾU USER BỊ KHÓA (isEnabled() == false)
                        System.out.println("JWT Filter: User " + userEmail + " is disabled/locked.");
                        SecurityContextHolder.clearContext();
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden
                        response.getWriter().write("Tài khoản của bạn đã bị khóa.");
                        return; // Dừng xử lý
                    }
                    // === KẾT THÚC SỬA ĐỔI ===
                    // =======================================================

                } else {
                    System.out.println("JWT Filter: Token invalid for user: " + userEmail);
                }
            } catch (Exception e) {
                System.out.println("JWT Filter: Error loading userDetails or validating token: " + e.getMessage());
                // Không throw, tiếp tục chain để tránh 500, nhưng auth không set → 403 nếu
                // require authenticated
            }
        } else {
            System.out.println("JWT Filter: Skipping auth - userEmail null or already authenticated");
        }

        filterChain.doFilter(request, response);
    }
}