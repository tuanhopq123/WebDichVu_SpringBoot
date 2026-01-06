export function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    // Hiển thị
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);

    // Tự động ẩn
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            container.removeChild(toast);
        }, 500);
    }, 3000);
}

export function formatTimeAgo(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);

    let interval = seconds / 31536000;
    if (interval > 1) return Math.floor(interval) + " năm trước";
    
    interval = seconds / 2592000;
    if (interval > 1) return Math.floor(interval) + " tháng trước";
    
    interval = seconds / 86400;
    if (interval > 1) return Math.floor(interval) + " ngày trước";
    
    interval = seconds / 3600;
    if (interval > 1) return Math.floor(interval) + " giờ trước";
    
    interval = seconds / 60;
    if (interval > 1) return Math.floor(interval) + " phút trước";
    
    return "Vừa xong";
}

export function showOrderDetails(order) {
    const modal = document.getElementById('order-details-modal');
    const modalBody = document.getElementById('modal-body-content');
    
    modalBody.innerHTML = `
        <p><strong>Dịch vụ:</strong> ${order.service.tenDichVu}</p>
        <p><strong>Khách hàng:</strong> ${order.user.hoTen} (${order.user.email})</p>
        <p><strong>SĐT Khách:</strong> ${order.sdt || order.user.sdt || '(Chưa cập nhật)'}</p>
        <p><strong>Thời gian hẹn:</strong> ${new Date(order.thoiGianDat).toLocaleString('vi-VN')}</p>
        <p><strong>Địa chỉ:</strong> ${order.diaChiDichVu}</p>
        <p><strong>Số lượng NV cần:</strong> ${order.soLuong}</p>
        <p><strong>Tổng tiền:</strong> ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(order.tongTien)}</p>
        <p><strong>Ghi chú:</strong> ${order.notes || '(Không có)'}</p>
    `;
    
    const closeModal = () => {
        modal.classList.remove('show');
    };

    document.getElementById('modal-close-x-btn').onclick = closeModal;
    document.getElementById('modal-close-footer-btn').onclick = closeModal;
    
    modal.onclick = (event) => {
        if (event.target === modal) {
            closeModal();
        }
    };

    modal.classList.add('show');
}