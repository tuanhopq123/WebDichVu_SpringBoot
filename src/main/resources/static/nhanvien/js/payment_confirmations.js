import { fetchWithAuthCheck } from './auth.js';
import { showToast } from './toast.js';

const currencyFormatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });
const COMMISSION_RATE = 0.30;

let pendingOrders = [];

export async function loadPaymentConfirmations() {
    const tableBody = document.getElementById('confirm-table-body');
    tableBody.innerHTML = '<tr><td colspan="5">Đang tải yêu cầu...</td></tr>';

    try {
        const response = await fetchWithAuthCheck('/api/assignments/payment-confirmations', {
            method: 'GET'
        });
        pendingOrders = await response.json();
        
        // Cập nhật badge
        document.getElementById('payment-confirm-badge').textContent = pendingOrders.length;
        document.getElementById('payment-confirm-badge').style.display = pendingOrders.length > 0 ? 'inline-block' : 'none';

        if (pendingOrders.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5">Không có khoản thanh toán nào đang chờ xác nhận.</td></tr>';
            return;
        }
        
        renderConfirmTable(pendingOrders);

    } catch (error) {
        showToast('Lỗi tải yêu cầu xác nhận: ' + error.message, 'error');
        tableBody.innerHTML = `<tr><td colspan="5">Lỗi: ${error.message}</td></tr>`;
    }
}

function renderConfirmTable(orders) {
    const tableBody = document.getElementById('confirm-table-body');
    tableBody.innerHTML = '';

    orders.forEach(order => {
        const commission = order.tongTien * COMMISSION_RATE;
        const row = document.createElement('tr');
        row.dataset.orderId = order.id;
        row.innerHTML = `
            <td>#${order.id}</td>
            <td>${order.service.tenDichVu}</td>
            <td>${currencyFormatter.format(order.tongTien)}</td>
            <td class="col-amount">${currencyFormatter.format(commission)}</td>
            <td>
                <button class="btn btn-success btn-sm btn-confirm-payment">
                    <i class="fas fa-check"></i> Đã nhận
                </button>
            </td>
        `;
        
        row.querySelector('.btn-confirm-payment').addEventListener('click', handleConfirmPayment);
        tableBody.appendChild(row);
    });
}

async function handleConfirmPayment(event) {
    const btn = event.currentTarget;
    const row = btn.closest('tr');
    const orderId = row.dataset.orderId;

    if (!confirm(`Xác nhận bạn đã nhận được tiền cho đơn hàng #${orderId}?`)) {
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

    try {
        const response = await fetchWithAuthCheck(`/api/assignments/payment-confirmations/${orderId}/confirm`, {
            method: 'POST'
        });
        
        const result = await response.json();
        showToast(result.message, 'success');
        
        // Xóa dòng này khỏi bảng
        row.remove();
        
        // Cập nhật lại badge
        pendingOrders = pendingOrders.filter(o => o.id != orderId);
        document.getElementById('payment-confirm-badge').textContent = pendingOrders.length;
        document.getElementById('payment-confirm-badge').style.display = pendingOrders.length > 0 ? 'inline-block' : 'none';

    } catch (error) {
        showToast('Lỗi xác nhận: ' + error.message, 'error');
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-check"></i> Đã nhận';
    }
}