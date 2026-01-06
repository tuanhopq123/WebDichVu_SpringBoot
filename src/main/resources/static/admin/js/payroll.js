import { fetchWithAuthCheck } from './api.js';
import { showToast } from './toast.js';

const currencyFormatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });
const COMMISSION_RATE = 0.30;

// STATE
let allPayrollData = [];
let filteredData = [];
let currentPage = 1;
const itemsPerPage = 5;
let currentEmployeeId = null;
let currentOrdersInModal = [];
let searchTimeout = null;

export function initPayrollPage() {
    loadPayrollSummary();
    setupListeners();
    
    // Xuất hàm ra window để gọi từ HTML onclick
    window.switchTab = switchTab;
    window.loadHistoryData = loadHistoryData;
    window.handleOpenDetailModal = handleOpenDetailModal;
}

function switchTab(tabName) {
    // Reset style nút
    document.getElementById('tab-pending').style.borderBottomColor = 'transparent';
    document.getElementById('tab-pending').style.color = '#666';
    document.getElementById('tab-history').style.borderBottomColor = 'transparent';
    document.getElementById('tab-history').style.color = '#666';

    // Active nút được chọn
    const activeBtn = document.getElementById(`tab-${tabName}`);
    activeBtn.style.borderBottomColor = '#007bff';
    activeBtn.style.color = '#007bff';

    // Ẩn hiện content
    document.getElementById('view-pending').style.display = 'none';
    document.getElementById('view-history').style.display = 'none';
    document.getElementById(`view-${tabName}`).style.display = 'block';

    // Load data nếu là tab history
    if (tabName === 'history') {
        // Set mặc định ngày (tháng này) nếu chưa có
        const fromInput = document.getElementById('history-from-date');
        const toInput = document.getElementById('history-to-date');
        if (!fromInput.value) {
            const today = new Date();
            const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
            fromInput.value = firstDay.toISOString().split('T')[0];
            toInput.value = today.toISOString().split('T')[0];
        }
        loadHistoryData();
    } else {
        loadPayrollSummary();
    }
}

async function loadPayrollSummary() {
    const tableBody = document.getElementById('payroll-summary-body');
    if(!tableBody) return;
    tableBody.innerHTML = '<tr><td colspan="6" class="text-center">Đang tải dữ liệu...</td></tr>';
            tableBody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align:center; padding: 20px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #007bff;"></i>
                    <div style="margin-top: 5px;">Đang tải dữ liệu...</div>
                </td>
            </tr>`;
    try {
        const response = await fetchWithAuthCheck('/api/admin/payroll/summary', { method: 'GET' });
        allPayrollData = await response.json();
        applyFiltersAndSort();
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="6" class="text-center" style="color:red">Lỗi: ${error.message}</td></tr>`;
    }
}

function applyFiltersAndSort() {
    const searchInput = document.getElementById('payroll-search');
    const sortSelect = document.getElementById('payroll-sort');
    if(!searchInput) return;

    const searchTerm = normalizeString(searchInput.value);
    const sortValue = sortSelect.value;

    filteredData = allPayrollData.filter(item => {
        const name = normalizeString(item.employeeName);
        const email = normalizeString(item.employeeEmail);
        return name.includes(searchTerm) || email.includes(searchTerm);
    });

    filteredData.sort((a, b) => {
        if (sortValue === 'amount-desc') return b.totalUnpaidAmount - a.totalUnpaidAmount;
        if (sortValue === 'name-asc') return a.employeeName.localeCompare(b.employeeName);
        return 0;
    });

    currentPage = 1;
    renderTable();
}

function renderTable() {
    const tableBody = document.getElementById('payroll-summary-body');
    const pageInfo = document.getElementById('page-info');
    const prevBtn = document.getElementById('prev-page-btn');
    const nextBtn = document.getElementById('next-page-btn');
    
    tableBody.innerHTML = '';
    if (filteredData.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center">Không có dữ liệu.</td></tr>';
        return;
    }

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const pageData = filteredData.slice(startIndex, endIndex);

    pageData.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td style="font-weight: 500;">${item.employeeName}</td>
            <td>${item.employeeEmail}</td>
            <td>${item.employeePhone || '---'}</td>
            <td class="text-center"><span class="badge" style="background:#17a2b8; color:white;">${item.totalOrders}</span></td>
            <td style="font-weight: bold; color: #dc3545;">${currencyFormatter.format(item.totalUnpaidAmount)}</td>
            <td class="text-center">
                <button class="btn btn-sm btn-primary btn-view-details" 
                        onclick="window.handleOpenDetailModal('${item.employeeId}', '${item.employeeName}')">
                    <i class="fas fa-list"></i> Chi tiết
                </button>
            </td>
        `;
        tableBody.appendChild(row);
    });

    if(pageInfo) pageInfo.textContent = `Trang ${currentPage} / ${Math.ceil(filteredData.length/itemsPerPage)}`;
    if(prevBtn) prevBtn.disabled = currentPage === 1;
    if(nextBtn) nextBtn.disabled = currentPage === Math.ceil(filteredData.length/itemsPerPage);
}

async function loadHistoryData() {
    const search = document.getElementById('history-search').value.trim();
    const status = document.getElementById('history-status').value; // Lấy giá trị status
    const fromDate = document.getElementById('history-from-date').value;
    const toDate = document.getElementById('history-to-date').value;
    const tbody = document.getElementById('payroll-history-body');

    // Không hiện loading nếu đang gõ phím (để UX mượt hơn), chỉ hiện khi load lần đầu
    if (!tbody.innerHTML.includes('tr')) {
         tbody.innerHTML = '<tr><td colspan="6" class="text-center">Đang tải...</td></tr>';
    }

    try {
        let url = `/api/admin/payroll/history?fromDate=${fromDate}&toDate=${toDate}&status=${status}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;

        const response = await fetchWithAuthCheck(url, { method: 'GET' });
        const orders = await response.json();

        tbody.innerHTML = '';
        if (orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Không tìm thấy dữ liệu.</td></tr>';
            return;
        }

        orders.forEach(order => {
            const comm = order.tongTien * COMMISSION_RATE;
            
            // Render Badge trạng thái đẹp mắt
            let statusBadge = '';
            if (order.employeePaymentStatus === 'PAID') {
                statusBadge = `<span class="badge" style="background:#28a745; color:white"><i class="fas fa-check"></i> Đã Trả</span>`;
            } else {
                statusBadge = `<span class="badge" style="background:#ffc107; color:#333"><i class="fas fa-clock"></i> Chờ NV Xác Nhận</span>`;
            }

            const row = document.createElement('tr');
            row.innerHTML = `
                <td><span style="background:#eee; padding:3px 6px; border-radius:4px; font-size:12px">#${order.id}</span></td>
                <td style="font-weight:500;">${order.employee.hoTen}</td>
                <td>${order.service.tenDichVu}</td>
                <td>${new Date(order.completedAt).toLocaleDateString('vi-VN')}</td>
                <td style="color:#28a745; font-weight:bold;">${currencyFormatter.format(comm)}</td>
                <td class="text-center">${statusBadge}</td>
            `;
            tbody.appendChild(row);
        });

    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-danger text-center">Lỗi: ${error.message}</td></tr>`;
    }
}

function handleOpenDetailModal(id, name) {
    currentEmployeeId = id;
    document.getElementById('payroll-employee-name').textContent = name;
    document.getElementById('payroll-detail-body').innerHTML = '';
    document.getElementById('payroll-total-amount').textContent = '0 đ';
    document.getElementById('payroll-detail-modal').style.display = 'flex';

    // Default date: Đầu năm -> Hôm nay (Để quét hết đơn cũ)
    const today = new Date();
    const startOfYear = new Date(today.getFullYear(), 0, 1);
    document.getElementById('payroll-from-date').value = startOfYear.toISOString().split('T')[0];
    document.getElementById('payroll-to-date').value = today.toISOString().split('T')[0];

    loadFilteredDetails();
}

async function loadFilteredDetails() {
    const fromDate = document.getElementById('payroll-from-date').value;
    const toDate = document.getElementById('payroll-to-date').value;
    
    try {
        const url = `/api/admin/payroll/details/${currentEmployeeId}?fromDate=${fromDate}&toDate=${toDate}`;
        const response = await fetchWithAuthCheck(url, { method: 'GET' });
        currentOrdersInModal = await response.json();
        
        const tbody = document.getElementById('payroll-detail-body');
        tbody.innerHTML = '';
        let total = 0;

        if (currentOrdersInModal.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center">Không có dữ liệu.</td></tr>';
        } else {
            currentOrdersInModal.forEach(order => {
                const comm = order.tongTien * COMMISSION_RATE;
                total += comm;
                const row = document.createElement('tr');
                row.innerHTML = `<td>#${order.id}</td><td>${order.service.tenDichVu}</td><td>${currencyFormatter.format(order.tongTien)}</td><td style="color:#28a745; font-weight:bold">${currencyFormatter.format(comm)}</td>`;
                tbody.appendChild(row);
            });
        }
        document.getElementById('payroll-total-amount').textContent = currencyFormatter.format(total);
        
        const payBtn = document.getElementById('payroll-submit-payment-btn');
        if(payBtn) payBtn.disabled = (currentOrdersInModal.length === 0);
    } catch (e) { console.error(e); }
}

// Event Listeners
function setupListeners() {
    document.getElementById('payroll-search')?.addEventListener('input', applyFiltersAndSort);
    document.getElementById('payroll-sort')?.addEventListener('change', applyFiltersAndSort);
    document.getElementById('payroll-filter-btn')?.addEventListener('click', loadFilteredDetails);

    document.getElementById('prev-page-btn')?.addEventListener('click', () => { if(currentPage>1){currentPage--; renderTable();} });

    const historySearchInput = document.getElementById('history-search');
    const historyStatusSelect = document.getElementById('history-status');

    if (historySearchInput) {
        historySearchInput.addEventListener('input', function() {
            // Debounce: Chỉ gọi API sau khi ngừng gõ 500ms
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                window.loadHistoryData();
            }, 500);
        });
    }

    if (historyStatusSelect) {
        historyStatusSelect.addEventListener('change', window.loadHistoryData);
    }
    
    document.getElementById('next-page-btn')?.addEventListener('click', () => { 
        if(currentPage < Math.ceil(filteredData.length/itemsPerPage)){currentPage++; renderTable();} 
    });

    document.getElementById('payroll-submit-payment-btn')?.addEventListener('click', async () => {
        if(!confirm('Xác nhận thanh toán?')) return;
        try {
            await fetchWithAuthCheck('/api/admin/payroll/request-payment', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    employeeId: currentEmployeeId,
                    fromDate: document.getElementById('payroll-from-date').value,
                    toDate: document.getElementById('payroll-to-date').value
                })
            });
            showToast('Thanh toán thành công!', 'success');
            document.getElementById('payroll-detail-modal').style.display = 'none';
            loadPayrollSummary();
        } catch(e) { showToast(e.message, 'error'); }
    });

    document.querySelector('[data-close-modal="payroll-detail-modal"]')?.addEventListener('click', () => {
        document.getElementById('payroll-detail-modal').style.display = 'none';
    });
}

function normalizeString(str) { return str ? str.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase() : ''; }