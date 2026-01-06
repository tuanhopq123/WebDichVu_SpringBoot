import { showToast } from './toast.js';

export async function fetchWithAuthCheck(url, options) {
    
    const jwtToken = localStorage.getItem('jwtToken');
    const authOptions = { ...options }; // Copy options
    if (jwtToken) {
        authOptions.headers = {
            ...authOptions.headers,
            'Authorization': `Bearer ${jwtToken}`
        };
    }

    // Gọi fetch
    const response = await fetch(url, authOptions);

    // Kiểm tra lỗi "Văng ra" (Bị khóa hoặc Hết hạn)
    if (response.status === 401 || response.status === 403) {
        showToast('Phiên đăng nhập hết hạn hoặc tài khoản đã bị khóa.', 'error');
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('userInfo');
        
        window.location.href = '/login.html'; 
        
        throw new Error('Unauthorized or Forbidden');
    }

    if (!response.ok) {
        try {
            // Cố gắng đọc lỗi JSON từ server
            const err = await response.json();
            throw new Error(err.message || err.error || `Lỗi ${response.status}`);
        } catch (e) {
            // Nếu server trả về text (như lỗi 500 HTML)
            throw new Error(`Lỗi máy chủ: ${response.status}`);
        }
    }

    return response;
}