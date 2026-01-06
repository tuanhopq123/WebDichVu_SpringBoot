function updateBadgeCount(count) {
    const badge = document.querySelector('.notification-badge');
    if (badge) {
        const num = parseInt(count) || 0;
        badge.textContent = num > 99 ? '99+' : num;
        badge.style.display = num > 0 ? 'flex' : 'none';
    }
}

function decrementBadgeCount() {
    let currentCount = parseInt(localStorage.getItem('unreadNotificationCount') || 0);
    if (currentCount > 0) {
        currentCount--;
        localStorage.setItem('unreadNotificationCount', currentCount);
        updateBadgeCount(currentCount);
    }
}

function resetBadgeCount() {
    localStorage.setItem('unreadNotificationCount', 0);
    updateBadgeCount(0);
}

function loadNotifications(token) {
    const cacheBuster = "t=" + new Date().getTime();
    
    // ⭐ QUAN TRỌNG: Dùng API /recent để lấy List thay vì Page
    fetch(`/api/notifications/recent?${cacheBuster}`, {
        method: 'GET',
        headers: { 
            'Authorization': `Bearer ${token}`,
            'Cache-Control': 'no-cache'
        },
        cache: 'no-store' 
    })
    .then(res => {
        if (!res.ok) throw new Error('Không thể tải thông báo');
        return res.json();
    })
    .then(data => {
        const list = document.getElementById('notification-list');
        if (!list) return; 

        // Kiểm tra dữ liệu an toàn
        const notifications = Array.isArray(data) ? data : (data.content || []);

        // Đếm số lượng chưa đọc
        // Kiểm tra cả trường 'read' và 'isRead' do JSON serialize có thể khác nhau
        const unreadCount = notifications.filter(n => {
            const isRead = (n.read !== undefined) ? n.read : n.isRead;
            return !isRead;
        }).length;
        
        // Lưu vào storage
        localStorage.setItem('unreadNotificationCount', unreadCount);
        
        // Cập nhật số trên chuông
        updateBadgeCount(unreadCount);

        // Hiển thị tối đa 5 thông báo trên popup
        const notificationsToDisplay = notifications.slice(0, 5);

        if (notificationsToDisplay.length > 0) {
            list.innerHTML = notificationsToDisplay.map(n => {
                // Xử lý icon
                let iconClass = 'fa-bell';
                if (n.type === 'ORDER') iconClass = 'fa-calendar-check';
                if (n.type === 'PROMOTION') iconClass = 'fa-percent';

                // Xử lý trạng thái đọc
                const isRead = (n.read !== undefined) ? n.read : n.isRead;

                return `
                <div class="notification-item ${isRead ? 'read' : 'unread'}" 
                     data-id="${n.id}" 
                     data-link="${n.link || '#'}">
                    
                    <div class="notification-icon">
                        <i class="fas ${iconClass}"></i>
                    </div>

                    <div class="notification-content">
                        <p>${n.message}</p> 
                        <span class="notification-time">${timeElapsedString(n.createdAt)}</span>
                    </div>
                </div>
            `}).join('');
        } else {
            list.innerHTML = '<div class="empty-notifications"><p>Bạn không có thông báo mới</p></div>';
        }
    })
    .catch(err => {
        console.error("Lỗi loadNotifications:", err);
    });
}

function timeElapsedString(date) {
    if (!date) return '';
    const now = new Date();
    const seconds = Math.floor((now - new Date(date)) / 1000);
    if (seconds < 60) return `Vừa xong`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes} phút trước`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} giờ trước`;
    const days = Math.floor(hours / 24);
    return `${days} ngày trước`;
}

function setupNotificationClick() {
    const authSection = document.getElementById('auth-section');
    if (!authSection) return;

    // Xóa event cũ để tránh duplicate nếu gọi nhiều lần (tùy chọn)
    authSection.onclick = null; 

    authSection.onclick = async function(event) { 
        const container = event.target.closest('.notification-container');
        
        // Xử lý đóng khi click ra ngoài vùng notification nhưng vẫn trong auth-section
        if (!container) {
            const activeContainer = authSection.querySelector('.notification-container.active');
            // Chỉ đóng nếu không click vào các phần tử tương tác khác
            if (activeContainer && !event.target.closest('.user-menu-container')) {
               // activeContainer.classList.remove('active'); // Comment lại để tránh xung đột logic click body
            }
            return;
        }

        // 1. Bấm vào biểu tượng chuông
        if (event.target.closest('.notification-icon')) {
            container.classList.toggle('active');
        }

        // 2. Bấm vào "Đánh dấu tất cả đã đọc" (trong popup)
        if (event.target.closest('.mark-all-read')) {
            event.preventDefault();
            await markAllNotificationsAsRead();
        }

        // 3. Bấm vào một thông báo cụ thể
        const item = event.target.closest('.notification-item');
        if (item) {
            event.preventDefault(); // Ngăn chuyển trang ngay lập tức
            const id = item.dataset.id;
            const link = item.dataset.link;
            
            // Nếu chưa đọc thì gọi API đánh dấu
            if (item.classList.contains('unread')) {
                await markNotificationAsRead(id, item); 
            }
            
            // Chuyển trang sau khi xử lý xong
            if (link && link !== '#') {
                window.location.href = link;
            }
        }
    }; 

    // Click ra ngoài body để đóng popup
    document.addEventListener('click', function(event) {
        const authSection = document.getElementById('auth-section');
        if (!authSection) return;
        const activeContainer = authSection.querySelector('.notification-container.active');
        
        // Nếu có popup đang mở và click không nằm trong auth-section
        if (activeContainer && !authSection.contains(event.target)) {
            activeContainer.classList.remove('active');
        }
    });
}

async function markNotificationAsRead(id, itemElement) {
    const token = localStorage.getItem('jwtToken');
    
    // Cập nhật giao diện ngay lập tức
    if (itemElement) {
        itemElement.classList.remove('unread');
        itemElement.classList.add('read');
    }
    
    // Giảm số đếm
    decrementBadgeCount();
    
    // Gửi API ngầm
    try {
        await fetch(`/api/notifications/${id}/read`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
    } catch (err) {
        console.error('Lỗi đánh dấu đã đọc:', err);
        // Nếu lỗi thì load lại để đồng bộ số đúng
        loadNotifications(token);
    }
}

async function markAllNotificationsAsRead() {
    const token = localStorage.getItem('jwtToken');
    if (!token) return;

    // Cập nhật giao diện popup
    const list = document.getElementById('notification-list');
    if (list) {
        list.querySelectorAll('.notification-item.unread').forEach(item => {
            item.classList.remove('unread');
            item.classList.add('read');
        });
    }
    
    // Reset số đếm
    resetBadgeCount();
    
    // Gửi API
    try {
        await fetch(`/api/notifications/read-all`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
    } catch (err) {
        console.error('Lỗi đánh dấu tất cả:', err);
        loadNotifications(token);
    }
}