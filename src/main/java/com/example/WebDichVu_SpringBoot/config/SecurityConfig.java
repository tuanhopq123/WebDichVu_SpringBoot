package com.example.WebDichVu_SpringBoot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;
        private final OAuth2SuccessHandler oAuth2SuccessHandler; // THÊM

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                // 2. CORS
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // 3. OAuth2 Login (Google)
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login.html")
                                                .successHandler(oAuth2SuccessHandler) // Tạo JWT + redirect
                                )
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/home.html",
                                                                "/error",
                                                                "/register.html",
                                                                "/login.html",
                                                                "/booking.html",
                                                                "/bookings_detail.html",
                                                                "/cart.html",
                                                                "/checkout.html",
                                                                "/payment.html",
                                                                "/thank_you.html",
                                                                "/api/chatbot",
                                                                "/review.html",
                                                                "/dashboard.html",
                                                                "/services.html",
                                                                "/introduce.html",
                                                                "/service_price_list.html",
                                                                "/contact.html",
                                                                "/notifications.html",
                                                                "/account.html",
                                                                "/my_bookings.html",
                                                                "/change_password.html",
                                                                "/service_detail.html",
                                                                "/admin/**",
                                                                "/nhanvien/**",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/assets/**",
                                                                "/favicon.ico",
                                                                "/api/auth/**",
                                                                "/api/services/**",
                                                                "/api/categories/**",
                                                                "/api/notifications",
                                                                "/api/reviews/**",
                                                                "/api/contact",
                                                                "/ws/**",
                                                                "/api/chat/**",
                                                                "/api/chat/history/**",
                                                                "/admin/content/**",
                                                                "/chat/**",
                                                                "/api/admin/payroll/**",
                                                                "/api/webhook/**")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                                                .requestMatchers("/ws/**").permitAll()

                                                .requestMatchers("/api/notifications/**").authenticated()
                                                .requestMatchers("/api/admin/orders/all").hasRole("ADMIN")
                                                .requestMatchers("/api/users/all").hasRole("ADMIN")
                                                .requestMatchers("/api/admin/payroll/**").hasAuthority("ADMIN")
                                                .requestMatchers("/api/admin/contacts/**").hasRole("ADMIN")
                                                .requestMatchers("/api/users/me").authenticated()
                                                .requestMatchers("/api/orders/**").authenticated()
                                                .requestMatchers("/api/users/**").authenticated()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/").setViewName("redirect:/home.html");
                registry.addViewController("/services").setViewName("redirect:/services.html");
        }
}