# WebDichVu_SpringBoot - Home Service Booking Platform

🌐 Live Demo
👉 Trải nghiệm ngay tại đây: [Bấm vào đây để xem Demo](https://do-an-booking-tuan.onrender.com)

> **⚠️ Lưu ý quan trọng:**
> Do dự án sử dụng gói Free Tier của Render, server sẽ tự động "ngủ" nếu không có truy cập.
> Lần tải trang đầu tiên có thể mất khoảng 50 - 60 giây để server khởi động lại. Mong bạn kiên nhẫn chờ đợi, các thao tác sau đó sẽ mượt mà bình thường.

> Mô tả: Hệ thống đặt lịch dịch vụ tại nhà, kết nối khách hàng và nhân viên kỹ thuật. Dự án được xây dựng theo kiến trúc Polyglot Persistence (Đa cơ sở dữ liệu), tối ưu hóa hiệu năng và trải nghiệm Real-time.

## I. Công Nghệ Sử Dụng (Tech Stack)

1. Backend Core
 Java 17
 Spring Boot 3.0

2. Database (Polyglot Persistence)
 MySQL: Quản lý dữ liệu quan hệ (User, Order, Transaction).
 MongoDB: Lưu trữ dữ liệu phi cấu trúc (Lịch sử Chat, Logs).
 Redis: Caching dữ liệu và quản lý TTL (Time-to-live) cho mã OTP.

3. Bảo mật & Giao tiếp
 Security: Spring Security, JWT (Stateless Authentication).
 Real-time: WebSocket (STOMP), SockJS.

4. Thanh toán (Payment Integration)
 VietQR: Tạo mã QR thanh toán động.
 Casso: Tự động hóa xử lý Webhook biến động số dư.

5. Frontend
 HTML5, Bootstrap 5.
 Thymeleaf Template Engine.
 JavaScript (ES6).

## II. Tính Năng Chính

### 1. Phân hệ Khách hàng & Nhân viên
 Đặt lịch thông minh: Hỗ trợ tìm kiếm dịch vụ, lựa chọn khung giờ và nhân viên phù hợp theo nhu cầu.
 Chat thời gian thực:
     Trao đổi trực tiếp giữa Khách và Nhân viên qua giao thức WebSocket.
     Sử dụng MongoDB để lưu trữ tin nhắn, tối ưu hóa tốc độ đọc/ghi dữ liệu lớn.
 Quy trình nhận việc:
     Nhân viên nhận thông báo việc làm mới tức thì.
     Hệ thống tự động kiểm soát trạng thái nhân viên (Rảnh/Bận) để phân phối đơn hàng hợp lý, tránh trùng lịch.

### 2. Thanh toán & Tự động hóa
 Thanh toán QR Code: Tích hợp VietQR giúp tạo mã thanh toán nhanh chóng.
 Xử lý Webhook: Hệ thống tự động xác nhận đơn hàng khi nhận biến động số dư từ ngân hàng (thông qua Casso), đảm bảo giao dịch an toàn với cơ chế xác thực chữ ký số.

### 3. Tối ưu hóa & Bảo mật
 Xử lý dữ liệu lớn: Ứng dụng Aggregation Pipeline của MongoDB để thống kê và gom nhóm tin nhắn.
 Hiệu năng: Sử dụng kỹ thuật `JOIN FETCH` trong Spring Data JPA để giải quyết triệt để vấn đề N+1 Query.
 Bảo mật:
     Phân quyền chặt chẽ (RBAC).
     Mã hóa mật khẩu.
     Cấu hình chống CSRF/CORS.

## III. Cài đặt & Chạy (Installation)

Yêu cầu môi trường:
 JDK 17 trở lên
 Maven
 MySQL, MongoDB, Redis (Khuyến khích cài đặt qua Docker)

Các bước thực hiện:

Bước 1: Clone dự án
bash
git clone [https://github.com/tuanhopq123/WebDichVu_SpringBoot.git](https://github.com/tuanhopq123/WebDichVu_SpringBoot.git)