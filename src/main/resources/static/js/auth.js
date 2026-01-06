const API_URL = '/api/auth/login';

function showToast(message, type) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.position = 'fixed';
        container.style.top = '20px';
        container.style.right = '20px';
        container.style.zIndex = '10000';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            toast.remove();
        }, 500);
    }, 4000);
}

function redirectByRole(vaiTro) {
    if (vaiTro === 'ADMIN') {
        return '/admin/admin_layout.html';
    } else if (vaiTro === 'NHAN_VIEN') {
        return '/nhanvien/employee_layout.html'; 
    } else {
        return '/home.html';
    }
}

function handleLogin(event) {
    event.preventDefault();

    const email = document.getElementById('emailLog').value;
    const matKhau = document.getElementById('matKhauLog').value;

    if (!email || !matKhau) {
        showToast('Vui lòng nhập đầy đủ email và mật khẩu.', 'error');
        return;
    }

    const submitBtn = document.querySelector('#loginForm button');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span>Loading...</span>';

    fetch(API_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, matKhau })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(data => {
                if (response.status === 401) {
                    throw new Error(data.error || 'Sai email hoặc mật khẩu.');
                }
                throw new Error(`Lỗi ${response.status}: Vui lòng thử lại.`);
            });
        }
        return response.json();
    })
    .then(data => {
        const jwtToken = data.token;
        const vaiTro = data.vaiTro;  

        if (!jwtToken || !vaiTro) {
            throw new Error('Phản hồi không hợp lệ từ server.');
        }

        localStorage.setItem('jwtToken', jwtToken);
        localStorage.setItem('userRole', vaiTro);
        showToast('Đăng nhập thành công! Đang chuyển hướng...', 'success');
        document.getElementById('loginForm').reset();

        const redirectUrl = redirectByRole(vaiTro);

        setTimeout(() => {
            window.location.href = redirectUrl;
        }, 1500);
    })
    .catch(error => {
        console.error('Lỗi đăng nhập:', error);
        showToast(error.message || 'Lỗi kết nối server.', 'error');
    })
    .finally(() => {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<span>Đăng Nhập</span>';
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    } else {
        console.error('Không tìm thấy form đăng nhập với ID "loginForm".');
    }
});

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const error = urlParams.get('error');
    const oauthToken = urlParams.get('oauth_token');
    
    //ĐỌC CẢ VAI TRÒ (ROLE)
    const oauthRole = urlParams.get('role');

    if (error) {
        showToast('Đăng nhập thất bại. Tài khoản đã bị khóa hoặc từ chối truy cập.', 'error');
        window.history.replaceState({}, document.title, window.location.pathname);
        return;
    }
    
    if (oauthToken && oauthRole) {
        //LƯU CẢ TOKEN VÀ ROLE
        localStorage.setItem('jwtToken', oauthToken);
        localStorage.setItem('userRole', oauthRole);
        
        showToast('Đăng nhập bằng Google thành công!', 'success');
        
        //XÓA TOKEN/ROLE KHỎI URL
        window.history.replaceState({}, document.title, '/login.html');
        
        const redirectUrl = redirectByRole(oauthRole);
        
        setTimeout(() => {
            window.location.href = redirectUrl;
        }, 1000);

    } else if (oauthToken && !oauthRole) {
        // Xử lý lỗi nếu server vì lý do nào đó không gửi Role
        console.error('OAuth Lỗi: Thiếu tham số "role" từ server.');
        showToast('Lỗi xác thực Google. Thiếu vai trò người dùng.', 'error');
        window.history.replaceState({}, document.title, '/login.html');
    }
    // Nếu không có token, không làm gì cả.
});