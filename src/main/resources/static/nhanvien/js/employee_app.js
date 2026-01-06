import { fetchWithAuthCheck } from './auth.js';
import { showToast } from './toast.js';
import { loadMyInvitations } from './my_invitations.js';
import { loadMyHistory } from './my_history.js';
import { loadMyProfile } from './my_profile.js';
import { loadMyEarnings } from './my_earnings.js';
import { loadPaymentConfirmations } from './payment_confirmations.js';

// Chạy khi DOM đã tải
document.addEventListener('DOMContentLoaded', () => {
    // 1. Kiểm tra Auth trước
    // 2. setupNavigation và setupLogout sẽ được gọi TỪ BÊN TRONG checkAuth
    checkAuthAndLoadApp();
});

function checkAuthAndLoadApp() {
    const token = localStorage.getItem('jwtToken');
    const contentDiv = document.getElementById('employee-dynamic-content');

    if (!token) {
        showToast('Vui lòng đăng nhập.', 'error');
        setTimeout(() => { window.location.href = '/login.html'; }, 1000);
        return;
    }

    // Dùng fetchWithAuthCheck đã import
    fetchWithAuthCheck('/api/users/me', { method: 'GET' })
        .then(response => response.json())
        .then(user => {
            if (user.vaiTro !== 'NHAN_VIEN') {
                localStorage.removeItem('jwtToken');
                contentDiv.innerHTML = '<div class="unauthorized-message">Bạn không phải là Nhân viên.</div>';
                showToast('Bạn không có quyền truy cập.', 'error');
                setTimeout(() => { window.location.href = '/login.html'; }, 2000);
                return;
            }

            document.getElementById('employee-name').textContent = user.hoTen;
            
            setupNavigation();
            setupLogout();

            // Tải nội dung trang ban đầu
            const initialPage = window.location.hash ? window.location.hash.substring(1) : 'my_invitations';
            loadContent(initialPage);
            document.querySelector(`.nav-item[data-page="${initialPage}"]`)?.classList.add('active');
        })
        .catch(error => {
            console.error('Lỗi xác thực:', error.message);
            contentDiv.innerHTML = '<div class="unauthorized-message">Phiên làm việc hết hạn.</div>';
        });
}

function loadContent(pageName) {
    const contentDiv = document.getElementById('employee-dynamic-content');
    contentDiv.innerHTML = '<p style="text-align:center;">Đang tải...</p>';

    fetch(`/nhanvien/content/${pageName}.html`)
        .then(response => {
            if (!response.ok) {
                return `<h1>Lỗi 404</h1><p>Không tìm thấy trang ${pageName}.html.</p>`;
            }
            return response.text();
        })
        .then(html => {
            contentDiv.innerHTML = html;
            history.pushState(null, '', `#${pageName}`);
            
            // Chạy JS cho trang tương ứng
            if (pageName === 'my_invitations') {
                loadMyInvitations();
            } else if (pageName === 'my_history') {
                loadMyHistory(); 
            } else if (pageName === 'my_profile') {
                loadMyProfile();
            }
            else if (pageName === 'my_earnings') {
                loadMyEarnings();
            }
            else if (pageName === 'payment_confirmations') {
                loadPaymentConfirmations();
            }
        })
        .catch(error => {
            contentDiv.innerHTML = `<h1>Lỗi tải trang</h1><p>${error.message}</p>`;
            showToast('Lỗi tải trang nội dung.', 'error');
        });
}

function setupNavigation() {
    const menuItems = document.querySelectorAll('.employee-nav .nav-item');
    menuItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            menuItems.forEach(i => i.classList.remove('active'));
            item.classList.add('active');

            const pageName = item.getAttribute('data-page');
            if (pageName) {
                loadContent(pageName);
            }
        });
    });
}

function setupLogout() {
    const logoutLink = document.getElementById('logout-link');
    if (logoutLink) {
        logoutLink.addEventListener('click', (e) => {
            e.preventDefault();
            localStorage.removeItem('jwtToken');
            showToast('Đăng xuất thành công.', 'success');
            setTimeout(() => {
                window.location.href = '/login.html';
            }, 1000);
        });
    } else {
        console.error("Lỗi: Không tìm thấy #logout-link.");
    }
}