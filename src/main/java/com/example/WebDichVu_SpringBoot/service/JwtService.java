package com.example.WebDichVu_SpringBoot.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    // Khóa bí mật
    @Value("${application.security.jwt.secret-key:A_VERY_LONG_AND_SECURE_SECRET_KEY_FOR_YOUR_DO_AN_CHUYEN_NGANH_PROJECT_32_BYTE_MINIMUM}")
    private String secretKey;

    // Thời gian hiệu lực của token (24 giờ)
    @Value("${application.security.jwt.expiration:86400000}")
    private long jwtExpiration;

    // Lấy tên người dùng (email) từ token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Lấy một claim cụ thể từ token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // 1. Lấy tất cả các quyền (Authorities) của người dùng (ví dụ: "ROLE_ADMIN")
        var authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 2. Thêm danh sách quyền vào Claims với khóa "roles"
        extraClaims.put("roles", authorities);

        return generateToken(extraClaims, userDetails);
    }

    // Tạo token với các claims bổ sung
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Thời gian hết hạn
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Xác thực token (Kiểm tra hết hạn và khớp username)
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Kiểm tra token đã hết hạn chưa
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Lấy thời gian hết hạn từ token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Giải mã toàn bộ claims
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Lấy Key bí mật để ký/xác thực token
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
