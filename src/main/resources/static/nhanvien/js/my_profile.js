import { getMyInfo } from './auth.js';
import { showToast } from './toast.js';

// Hàm chính
export async function loadMyProfile() {
    try {
        // 1. Gọi API /api/users/me
        const user = await getMyInfo();

        // 2. Điền thông tin vào các thẻ
        document.getElementById('profile-avatar').src = user.avatarURL || '/assets/avatar/default-avatar.png';
        document.getElementById('profile-name-avatar').textContent = user.hoTen;
        
        document.getElementById('profile-email').textContent = user.email;
        document.getElementById('profile-sdt').textContent = user.sdt || '(Chưa cập nhật)';
        document.getElementById('profile-address').textContent = user.diaChi || '(Chưa cập nhật)';
        
        // Xử lý Trạng thái làm việc
        const statusBadge = document.getElementById('profile-work-status');
        if (user.trangThaiLamViec === 'RANH') {
            statusBadge.textContent = 'Đang rảnh';
            statusBadge.className = 'status-badge status-RANH';
        } else if (user.trangThaiLamViec === 'BAN') {
            statusBadge.textContent = 'Đang bận';
            statusBadge.className = 'status-badge status-BAN';
        } else {
            statusBadge.textContent = 'N/A';
            statusBadge.className = 'status-badge';
        }
        
    } catch (error) {
        console.error("Lỗi khi tải hồ sơ:", error);
        showToast("Không thể tải hồ sơ của bạn.", "error");
    }
}