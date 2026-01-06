import { showToast } from './toast.js';
import { initCategoryPage } from './categories.js';
import { initServicePage } from './services.js';
import { initOrderPage } from './orders.js';
import { initUserPage } from './users.js';
import { fetchWithAuthCheck } from './api.js';
import { initDashboardPage } from './dashboard.js';
import { initEmployeePage } from './employees.js';
import { loadAssignmentsPage } from './assignments.js';
import { initPayrollPage } from './payroll.js';
import { initContactsPage } from './contacts.js';
import { initServiceChat } from './admin_chat.js';

export function setupRefreshButton() {
    const refreshBtn = document.getElementById('refresh-data-btn');
    
    if (!refreshBtn) {
        console.error("Lỗi: Không tìm thấy nút #refresh-data-btn");
        return;
    }

    refreshBtn.addEventListener('click', () => {
        const currentPage = window.location.hash ? window.location.hash.substring(1) : 'dashboard';
        
        // Cần đảm bảo hàm showToast và loadContent có thể được truy cập từ đây
        // (Chúng nên được định nghĩa trong cùng file này)
        showToast(`Đang tải lại dữ liệu trang ${currentPage}...`, 'success');
        loadContent(currentPage);
    });
}

function loadContent(pageName) {
    const contentDiv = document.getElementById('dynamic-content');
    contentDiv.innerHTML = '<p style="text-align:center;">Đang tải...</p>';

    cleanupListeners();

    fetch(`/admin/content/${pageName}.html`)
        .then(response => {
            if (!response.ok) {
                return `<h1>Lỗi 404</h1><p>Không tìm thấy nội dung cho trang ${pageName}.</p>`;
            }
            return response.text();
        })
        .then(html => {
            contentDiv.innerHTML = html;
            history.pushState(null, '', `#${pageName}`);
            if (pageName === 'services') {
                initServicePage();
            } else if (pageName === 'users') {
                initUserPage();
            } else if (pageName === 'categories') {
                initCategoryPage();
            } else if (pageName === 'orders') {
                initOrderPage();
            } else if (pageName === 'dashboard') {
                initDashboardPage();
            }
            else if (pageName === 'employees') {
                initEmployeePage();
            }
            else if (pageName === 'assignments') {
                loadAssignmentsPage();
            }
            else if (pageName === 'payroll') {
                initPayrollPage();
            }
            else if (pageName === 'contacts') {
                initContactsPage();
            }
            else if (pageName === 'livechat') {
                initServiceChat();
            }
        })
        .catch(error => {
            contentDiv.innerHTML = `<h1>Lỗi tải trang</h1><p>Đã xảy ra lỗi khi tải nội dung: ${error.message}</p>`;
            showToast('Lỗi tải trang nội dung.', 'error');
        });
}

let handleButtonClick = null;
let handleServiceClick = null;
let handleCategoryClick = null;
let handleOrderClick = null;

function cleanupListeners() {
    if (handleButtonClick) {
        document.removeEventListener('click', handleButtonClick, true);
    }
    if (handleServiceClick) {
        document.removeEventListener('click', handleServiceClick);
    }
    if (handleCategoryClick) {
        document.removeEventListener('click', handleCategoryClick);
    }
    if (handleOrderClick) {
        document.removeEventListener('click', handleOrderClick);
    }
    console.log('DEBUG: Đã xóa tất cả listener.');
}

function setupNavigation() {
    const menuItems = document.querySelectorAll('.sidebar-menu .menu-item');
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
    console.log("DEBUG: setupLogout() đã được gọi.");
    const logoutLink = document.getElementById('logout-link');

    if (logoutLink) {
        console.log("DEBUG: Phần tử #logout-link đã được tìm thấy.");
        logoutLink.addEventListener('click', (e) => {
            console.log("DEBUG: Sự kiện click Đăng xuất đã kích hoạt.");
            e.preventDefault();
            localStorage.removeItem('jwtToken');
            showToast('Đăng xuất Admin thành công. Đang chuyển hướng...', 'success');
            setTimeout(() => {
                window.location.href = '/login.html';
            }, 1000);
        });
    } else {
        console.error("Lỗi: Không tìm thấy phần tử #logout-link.");
    }
}

export function createSearchString(text) {
    if (!text) return '';
    return text
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '') // Xóa dấu
        .toLowerCase() // Chuyển sang chữ thường
        .replace(/[0-9]/g, '') // Xóa số
        .replace(/\s+/g, ''); // Xóa khoảng trắng
}

function checkAuthAndLoadAdminInfo() {
    const token = localStorage.getItem('jwtToken');
    const adminNameElement = document.getElementById('admin-name');
    const contentDiv = document.getElementById('dynamic-content');
    console.log('DEBUG: checkAuthAndLoadAdminInfo called, token:', token);

    if (!token) {
        showToast('Vui lòng đăng nhập với quyền Admin.', 'error');
        setTimeout(() => { window.location.href = '/login.html'; }, 1000);
        return;
    }

    fetchWithAuthCheck('/api/users/me', {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(user => {
            if (user.vaiTro !== 'ADMIN') {
                localStorage.removeItem('jwtToken');
                contentDiv.innerHTML = '<div class="unauthorized-message">Truy cập bị từ chối. Bạn không phải là Admin. Đang chuyển hướng...</div>';
                showToast('Bạn không có quyền Admin.', 'error');
                setTimeout(() => { window.location.href = '/dashboard.html'; }, 2000);
                return;
            }

            adminNameElement.textContent = user.hoTen || 'Admin';
            setupNavigation();
            const initialPage = window.location.hash ? window.location.hash.substring(1) : 'dashboard';
            loadContent(initialPage);
            document.querySelector(`.menu-item[data-page="${initialPage}"]`)?.classList.add('active');
        })
        .catch(error => {
            console.error('LỖI PHIÊN LÀM VIỆC:', error.message);
            if (error.message.includes('Token không hợp lệ hoặc hết hạn')) {
                localStorage.removeItem('jwtToken');
                contentDiv.innerHTML = '<div class="unauthorized-message">Phiên làm việc Admin hết hạn hoặc không hợp lệ. Đang chuyển hướng...</div>';
                showToast('Phiên làm việc Admin hết hạn.', 'error');
                setTimeout(() => { window.location.href = '/login.html'; }, 2000);
            } else {
                showToast('Không thể هستند thông tin người dùng: ' + error.message, 'error');
                setupNavigation();
                const initialPage = window.location.hash ? window.location.hash.substring(1) : 'dashboard';
                loadContent(initialPage);
                document.querySelector(`.menu-item[data-page="${initialPage}"]`)?.classList.add('active');
            }
        });
}

document.addEventListener('DOMContentLoaded', () => {
    setupLogout();
    checkAuthAndLoadAdminInfo();
    console.log("DEBUG: DOMContentLoaded hoàn tất và setup đã chạy.");
});

export { loadContent, setupNavigation, setupLogout, checkAuthAndLoadAdminInfo, cleanupListeners };