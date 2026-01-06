import { showToast } from './toast.js';

export async function fetchWithAuthCheck(url, options) {
    const jwtToken = localStorage.getItem('jwtToken');
    const authOptions = { ...options }; 

    if (jwtToken) {
        authOptions.headers = {
            ...authOptions.headers,
            'Authorization': `Bearer ${jwtToken}`
        };
    }

    const response = await fetch(url, authOptions);

    if (response.status === 401 || response.status === 403) {
        showToast('Phiên đăng nhập hết hạn hoặc tài khoản đã bị khóa.', 'error');
        localStorage.removeItem('jwtToken');
        
        window.location.href = '/login.html'; 
        
        throw new Error('Unauthorized or Forbidden');
    }

    if (!response.ok) {
        try {
            const err = await response.json();
            throw new Error(err.message || err.error || `Lỗi ${response.status}`);
        } catch (e) {
            throw new Error(`Lỗi máy chủ: ${response.status}`);
        }
    }
    return response;
}

export async function getMyInfo() {
    try {
        const response = await fetchWithAuthCheck('/api/users/me', {
            method: 'GET'
        });
        
        return await response.json();

    } catch (error) {
        console.error("Lỗi getMyInfo:", error);
        throw error;
    }
}