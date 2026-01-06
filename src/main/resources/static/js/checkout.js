const API_BASE_URL = '/api';
let cartData = null;
let currentUser = null;

const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
};

const truncateText = (text, maxWords) => {
    if (!text) return 'Không có mô tả';
    const words = text.trim().split(/\s+/);
    return words.length > maxWords ? words.slice(0, maxWords).join(' ') + '...' : text;
};

const getOptimizedImage = (url) => {
    if (!url || !url.includes('res.cloudinary.com')) {
        return url || '/assets/images/service-default.jpg';
    }
    return url.replace('/upload/', '/upload/w_400,h_300,c_fill,q_auto,f_auto/');
};

const showLoading = () => {
    document.getElementById('loadingOverlay').style.display = 'flex';
};

const hideLoading = () => {
    document.getElementById('loadingOverlay').style.display = 'none';
};

const getAuthHeaders = () => {
    const token = localStorage.getItem('jwtToken');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
};

const checkAuth = () => {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        window.location.href = '/login.html';
        return false;
    }
    return true;
};

const els = {
    addressInput: document.getElementById("address"),
    suggestionsBox: document.getElementById("suggestions"),
    getLocationBtn: document.getElementById("getCurrentLocation"),
    dateInput: document.getElementById("booking_date"),
    timeInput: document.getElementById("booking_time"),
    form: document.getElementById("checkoutForm"),
    submitBtn: document.getElementById("submitCheckout")
};

// Setup minimum date (+5 days)
const setupMinDate = () => {
    const minDate = new Date();
    minDate.setDate(minDate.getDate() + 5);
    els.dateInput.setAttribute('min', minDate.toISOString().split('T')[0]);
};

// Configure Flatpickr
flatpickr(els.timeInput, {
    enableTime: true,
    noCalendar: true,
    dateFormat: "h:i K",
    minTime: "08:00",
    maxTime: "20:00",
    defaultDate: "10:00 AM",
    minuteIncrement: 15
});

// Load Auth Section
const loadAuthSection = () => {
    const token = localStorage.getItem('jwtToken');
    const authSection = document.getElementById('auth-section');

    if (!token) {
        window.location.href = '/login.html';
        return;
    }

    fetch('/api/users/me', {
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(res => {
        if (!res.ok) throw new Error('Unauthorized');
        return res.json();
    })
    .then(user => {
        // Lưu lại user nếu cần dùng ở nơi khác
        currentUser = user;

        const avatar = user.avatarURL || '/assets/avatar/default-avatar.png';
        const displayName = user.hoTen || 'Người dùng';

        authSection.innerHTML = `
            <div class="header-actions">
                <div class="notification-container">
                    <div class="notification-icon">
                        <i class="fas fa-bell"></i>
                        <span class="notification-badge" style="display: none;"></span>
                    </div>
                    <div class="notification-dropdown">
                        <div class="notification-header">
                            <h3>Thông báo</h3>
                            <button class="mark-all-read">Đánh dấu đã đọc</button>
                        </div>
                        <div class="notification-list" id="notification-list"></div>
                        <div class="notification-footer">
                            <a href="/notifications.html">Xem tất cả</a>
                        </div>
                    </div>
                </div>

                <div class="user-menu-container">
                    <div class="user-menu-trigger">
                        <img src="${avatar}" alt="Avatar" class="user-avatar">
                        <span class="user-name">${displayName}</span>
                        <i class="fas fa-chevron-down"></i>
                    </div>
                    <div class="user-dropdown">
                        <a href="/account.html" class="user-dropdown-item"><i class="fas fa-user"></i> Hồ sơ</a>
                        <a href="/my_bookings.html" class="user-dropdown-item"><i class="fas fa-calendar-check"></i> Đơn đặt</a>
                        <a href="/cart.html" class="user-dropdown-item"><i class="fas fa-shopping-cart"></i> Giỏ hàng của tôi</a>
                        <a href="/change_password.html" class="user-dropdown-item"><i class="fas fa-key"></i> Đổi mật khẩu</a>
                        <div class="dropdown-divider"></div>
                        <a href="#" class="user-dropdown-item logout" onclick="logout(event)">
                            <i class="fas fa-sign-out-alt"></i> Đăng xuất
                        </a>
                    </div>
                </div>
            </div>`;

        // Điền thông tin vào form đặt lịch
        document.getElementById('fullname').value = user.hoTen || '';
        document.getElementById('phone').value    = user.sdt || '';
        document.getElementById('address').value  = user.diaChi || '';
        document.getElementById('user_id').value  = user.id;

        const initialCount = localStorage.getItem('unreadNotificationCount') || 0;
        if (typeof updateBadgeCount === 'function') updateBadgeCount(initialCount);
        if (typeof loadNotifications === 'function') loadNotifications(token);
        if (typeof setupNotificationClick === 'function') setupNotificationClick();
    })
    .catch(err => {
        console.error(err);
        localStorage.removeItem('jwtToken');
        window.location.href = '/login.html';
    });
};

window.logout = (event) => {
    event.preventDefault();
    localStorage.removeItem('jwtToken');
    window.location.href = '/login.html';
};

// Load Cart Data
const loadCartData = async () => {
    if (!checkAuth()) return;
    
    showLoading();
    try {
        const response = await fetch(`${API_BASE_URL}/cart`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) throw new Error('Failed to load cart');
        
        cartData = await response.json();
        
        if (!cartData || !cartData.cartItems || cartData.cartItems.length === 0) {
            alert('Giỏ hàng trống. Vui lòng thêm dịch vụ trước khi thanh toán.');
            window.location.href = '/services.html';
            return;
        }
        
        renderCartSummary();
        calculateTotals();
    } catch (error) {
        console.error('Error loading cart:', error);
        alert('Không thể tải giỏ hàng. Vui lòng thử lại.');
        window.location.href = '/cart.html';
    } finally {
        hideLoading();
    }
};

const renderCartSummary = () => {
    const container = document.getElementById('cartItemsSummary');
    const serviceCount = document.getElementById('service-count');
    
    if (!cartData || !cartData.cartItems) return;
    
    serviceCount.textContent = cartData.cartItems.length;
    
    container.innerHTML = cartData.cartItems.map(item => `
        <div class="cart-summary-item">
            <div class="cart-summary-image">
                <img src="${getOptimizedImage(item.service.imageURL)}" 
                     alt="${item.service.tenDichVu}" 
                     loading="lazy">
            </div>
            <div class="cart-summary-info">
                <div class="cart-summary-name">${truncateText(item.service.tenDichVu, 6)}</div>
                <div class="cart-summary-quantity">Số lượng: ${item.quantity}</div>
            </div>
            <div class="cart-summary-price">
                ${formatCurrency(item.service.giaCoBan * item.quantity)}
            </div>
        </div>
    `).join('');
};

const calculateTotals = () => {
    if (!cartData || !cartData.cartItems || cartData.cartItems.length === 0) {
        document.getElementById('totalAmount').textContent = '0 đ';
        return;
    }

    const total = cartData.cartItems.reduce((sum, item) => {
        return sum + (item.service.giaCoBan * item.quantity);
    }, 0);

    document.getElementById('totalAmount').textContent = formatCurrency(total);
};

let debounceTimer;
els.addressInput.addEventListener("input", (e) => {
    clearTimeout(debounceTimer);
    const query = e.target.value.trim();
    
    if (query.length < 3) {
        els.suggestionsBox.innerHTML = "";
        return;
    }
    
    debounceTimer = setTimeout(async () => {
        try {
            const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&addressdetails=1&limit=5&countrycodes=vn`);
            const data = await res.json();
            
            els.suggestionsBox.innerHTML = data.length 
                ? data.map(p => `<div class="suggestion-item" onclick="selectAddress('${p.display_name.replace(/'/g, "\\'")}')">${p.display_name}</div>`).join('')
                : '<div style="padding:10px;">Không tìm thấy kết quả</div>';
        } catch (e) {
            console.error('Address search error:', e);
        }
    }, 400);
});

window.selectAddress = (addr) => {
    els.addressInput.value = addr;
    els.suggestionsBox.innerHTML = "";
};

document.addEventListener("click", (e) => {
    if (!els.addressInput.contains(e.target) && !els.suggestionsBox.contains(e.target)) {
        els.suggestionsBox.innerHTML = "";
    }
});

// Get Current Location
els.getLocationBtn.addEventListener("click", () => {
    if (!navigator.geolocation) {
        alert("Trình duyệt không hỗ trợ định vị.");
        return;
    }
    
    els.getLocationBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    
    navigator.geolocation.getCurrentPosition(
        async (pos) => {
            try {
                const { latitude, longitude } = pos.coords;
                const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&zoom=18&addressdetails=1`);
                const data = await res.json();
                if (data.display_name) {
                    els.addressInput.value = data.display_name;
                }
            } catch (e) {
                alert("Không lấy được địa chỉ.");
            } finally {
                els.getLocationBtn.innerHTML = '<i class="fas fa-location-arrow"></i>';
            }
        },
        () => {
            alert("Không thể truy cập vị trí.");
            els.getLocationBtn.innerHTML = '<i class="fas fa-location-arrow"></i>';
        }
    );
});

document.getElementById('applyPromoBtn').addEventListener('click', async () => {
   
});

els.submitBtn.addEventListener("click", async (e) => {
    e.preventDefault();
    
    // Validate form
    if (!els.form.checkValidity()) {
        els.form.reportValidity();
        return;
    }
    
    const originalBtnText = els.submitBtn.innerHTML;
    els.submitBtn.disabled = true;
    els.submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xử lý...';
    showLoading();

    try {
        const formData = new FormData(els.form);
        
        // Validate date
        const selectedDate = new Date(formData.get("booking_date") || els.dateInput.value);
        const minDate = new Date();
        minDate.setDate(minDate.getDate() + 5);
        minDate.setHours(0, 0, 0, 0);
        selectedDate.setHours(0, 0, 0, 0);
        
        if (selectedDate < minDate) {
            throw new Error("Vui lòng chọn ngày từ 5 ngày sau hôm nay.");
        }

        // Validate phone
        const phone = document.getElementById("phone").value;
        if (!/^(0[0-9]{9,10})$/.test(phone)) {
            throw new Error("Số điện thoại không hợp lệ.");
        }

        // Convert time to 24h format
        const timeStr = els.timeInput.value;
        const [time, modifier] = timeStr.split(' ');
        let [hours, minutes] = time.split(':');
        
        if (hours === '12') hours = '00';
        if (modifier === 'PM') hours = parseInt(hours, 10) + 12;
        
        const hourInt = parseInt(hours);
        if (hourInt < 8 || hourInt >= 20) {
            throw new Error("Vui lòng đặt trong giờ làm việc (8h-20h)");
        }

        const formattedTime = `${hours.toString().padStart(2, '0')}:${minutes}`;
        const bookingDateTime = `${els.dateInput.value}T${formattedTime}`;

        // Calculate total
        const totalAmount = cartData.cartItems.reduce((sum, item) => {
            return sum + (item.service.giaCoBan * item.quantity);
        }, 0);

        const orderPayloads = [];
        
        cartData.cartItems.forEach(item => {
            // Nếu quantity = 2, vòng lặp chạy 2 lần, push 2 payload giống nhau vào list
            for (let i = 0; i < item.quantity; i++) {
                orderPayloads.push({
                    userId: currentUser.id,
                    serviceId: item.service.id,
                    thoiGianDat: bookingDateTime,
                    diaChiDichVu: els.addressInput.value,
                    // Tính tiền cho 1 đơn lẻ
                    tongTien: item.service.giaCoBan, 
                    phuongThucThanhToan: document.querySelector('input[name="payment_method"]:checked').value,
                    trangThai: "CHUA_XU_LY",
                    notes: document.getElementById('notes').value || '',
                    sdt: phone
                });
            }
        });

        const token = localStorage.getItem('jwtToken');
        
        // Cách dùng Promise.all (Gửi đồng thời)
        const orderPromises = orderPayloads.map(payload => 
            fetch(`${API_BASE_URL}/orders`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(payload)
            })
        );

        const responses = await Promise.all(orderPromises);
        
        const failedRequests = responses.filter(r => !r.ok);
        
        if (failedRequests.length > 0) {
            // Có ít nhất 1 đơn bị lỗi
            console.error("Số đơn lỗi:", failedRequests.length);
            throw new Error(`Có ${failedRequests.length} đơn hàng không tạo được. Vui lòng kiểm tra lại.`);
        }

        const createdOrders = await Promise.all(responses.map(res => res.json()));
        const orderIds = createdOrders.map(order => order.id); // Danh sách [101, 102, 103...]

        await fetch(`${API_BASE_URL}/cart`, { 
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        const paymentMethod = document.querySelector('input[name="payment_method"]:checked').value;
        
        if (paymentMethod === 'TIEN_MAT') {
            window.location.href = `/thank_you.html?booking_id=${orderIds[0]}`;
        } else {
            const idString = orderIds.join(',');
            window.location.href = `/payment.html?order_ids=${idString}&is_group=true`;
        }

    } catch (err) {
        console.error('Checkout error:', err);
        alert(err.message || 'Có lỗi xảy ra. Vui lòng thử lại.');
        els.submitBtn.disabled = false;
        els.submitBtn.innerHTML = originalBtnText;
        hideLoading();
    }
});

document.addEventListener('DOMContentLoaded', () => {
    if (!checkAuth()) return;
    
    setupMinDate();
    loadAuthSection();
    loadCartData();
});
