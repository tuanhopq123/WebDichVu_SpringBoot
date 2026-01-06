import { fetchWithAuthCheck } from './auth.js';
import { showToast } from './toast.js';
import { showOrderDetails } from './utils.js';

let allMyHistory = [];
let currentPage = 1;
const pageSize = 10;
let currentMonth = "all";
let currentYear = new Date().getFullYear();

// Tỷ lệ hoa hồng
const COMMISSION_RATE = 0.30; 

// Hàm định dạng tiền tệ
const currencyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
});

export async function loadMyEarnings() {
    console.log("Đang tải trang Thu nhập...");
    
    // 1. Khởi tạo bộ lọc (Dropdowns)
    setupFilters();
    
    // 2. Gán sự kiện cho bộ lọc
    setupFilterListeners();
    
    // 3. Tải dữ liệu
    const tableBody = document.getElementById('earnings-table-body');
    tableBody.innerHTML = '<tr><td colspan="6"><p>Đang tải dữ liệu thu nhập...</p></td></tr>';

    try {
        const response = await fetchWithAuthCheck('/api/assignments/my-history', {
            method: 'GET'
        });
        if (!response.ok) throw new Error('Không thể tải lịch sử.');

        allMyHistory = await response.json();
        
        // 4. Tải xong, render trang
        renderPage(); 

    } catch (error) {
        console.error('Lỗi tải thu nhập:', error);
        tableBody.innerHTML = '<tr><td colspan="6"><p>Lỗi khi tải dữ liệu. Vui lòng thử lại.</p></td></tr>';
    }
}

function renderPage() {
    
    // 1. Lọc (Filter)
    let filteredOrders = allMyHistory
        // Chỉ lấy các đơn đã HOAN_THANH
        .filter(oa => oa.order.trangThai === 'HOAN_THANH')
        // Lọc theo Năm
        .filter(oa => {
            const orderYear = new Date(oa.order.thoiGianDat).getFullYear();
            return orderYear === parseInt(currentYear);
        })
        // Lọc theo Tháng (nếu "all" thì bỏ qua)
        .filter(oa => {
            if (currentMonth === "all") return true;
            const orderMonth = new Date(oa.order.thoiGianDat).getMonth() + 1; // getMonth() (0-11)
            return orderMonth === parseInt(currentMonth);
        });

    // 2. Tính toán Thống kê (cho 3 thẻ)
    let totalEarned = 0;
    let totalPaid = 0;
    let totalUnpaid = 0;

    filteredOrders.forEach(oa => {
        const order = oa.order;
        const commission = order.tongTien * COMMISSION_RATE;
        
        totalEarned += commission;
        
        if (order.employeePaymentStatus === 'PAID') {
            totalPaid += commission;
        } else { // UNPAID
            totalUnpaid += commission;
        }
    });

    // Cập nhật 3 thẻ thống kê
    document.getElementById('stat-total-earned').textContent = currencyFormatter.format(totalEarned);
    document.getElementById('stat-paid').textContent = currencyFormatter.format(totalPaid);
    document.getElementById('stat-unpaid').textContent = currencyFormatter.format(totalUnpaid);

    // 3. Phân trang (Paginate)
    const totalItems = filteredOrders.length;
    const totalPages = Math.ceil(totalItems / pageSize);
    if (currentPage > totalPages) currentPage = totalPages;
    if (currentPage < 1) currentPage = 1;

    const startIndex = (currentPage - 1) * pageSize;
    const paginatedItems = filteredOrders.slice(startIndex, startIndex + pageSize);

    // 4. Vẽ (Render)
    renderEarningsTable(paginatedItems);
    renderPagination(totalPages, totalItems);
}

function renderEarningsTable(orders) {
    const tableBody = document.getElementById('earnings-table-body');
    tableBody.innerHTML = ""; 

    if (orders.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6"><p>Không tìm thấy đơn hàng hoàn thành nào.</p></td></tr>';
        return;
    }

    orders.forEach(oa => {
        const order = oa.order;
        const commission = order.tongTien * COMMISSION_RATE;
        const row = document.createElement('tr');
        
        row.innerHTML = `
            <td>${order.service.tenDichVu}</td>
            <td>${order.user.hoTen}</td>
            <td class="col-amount">${currencyFormatter.format(order.tongTien)}</td>
            <td class="col-amount">${currencyFormatter.format(commission)}</td>
            <td class="col-status">${renderPaymentStatus(order.employeePaymentStatus)}</td>
            <td>
                <button class="btn btn-sm btn-secondary btn-view-details">
                    <i class="fas fa-eye"></i> Xem
                </button>
            </td>
        `;

        // Gán sự kiện cho nút "Xem" (dùng chung modal)
        row.querySelector('.btn-view-details').addEventListener('click', () => {
            showOrderDetails(order); 
        });

        tableBody.appendChild(row);
    });
}

function renderPagination(totalPages, totalItems) {
    const container = document.getElementById('earnings-pagination');
    container.innerHTML = ""; 

    if (totalItems === 0) return;

    // (Code hàm renderPagination y hệt như của file my_history.js)
    // Nút "Trước"
    const prevButton = document.createElement('button');
    prevButton.innerHTML = "&laquo; Trước";
    prevButton.className = "btn btn-sm page-btn";
    prevButton.disabled = (currentPage === 1);
    prevButton.addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
        }
    });
    container.appendChild(prevButton);

    // Thông tin trang
    const pageInfo = document.createElement('span');
    pageInfo.className = "page-info";
    pageInfo.textContent = `Trang ${currentPage} / ${totalPages || 1}`;
    container.appendChild(pageInfo);

    // Nút "Sau"
    const nextButton = document.createElement('button');
    nextButton.innerHTML = "Sau &raquo;";
    nextButton.className = "btn btn-sm page-btn";
    nextButton.disabled = (currentPage === totalPages);
    nextButton.addEventListener('click', () => {
        if (currentPage < totalPages) {
            currentPage++;
            renderPage();
        }
    });
    container.appendChild(nextButton);
}

function setupFilters() {
    const monthFilter = document.getElementById('earnings-month-filter');
    const yearFilter = document.getElementById('earnings-year-filter');
    
    // Thêm 12 tháng
    for (let i = 1; i <= 12; i++) {
        const option = document.createElement('option');
        option.value = i;
        option.textContent = `Tháng ${i}`;
        monthFilter.appendChild(option);
    }
    
    // Thêm 3 năm (hiện tại, -1, -2)
    const currentYearValue = new Date().getFullYear();
    for (let i = 0; i < 3; i++) {
        const year = currentYearValue - i;
        const option = document.createElement('option');
        option.value = year;
        option.textContent = `Năm ${year}`;
        yearFilter.appendChild(option);
    }
    
    // Set giá trị mặc định (tháng hiện tại)
    monthFilter.value = new Date().getMonth() + 1;
    yearFilter.value = currentYearValue;
    currentMonth = monthFilter.value;
    currentYear = yearFilter.value;
}

function setupFilterListeners() {
    document.getElementById('earnings-month-filter').addEventListener('change', (e) => {
        currentMonth = e.target.value;
        currentPage = 1;
        renderPage();
    });

    document.getElementById('earnings-year-filter').addEventListener('change', (e) => {
        currentYear = e.target.value;
        currentPage = 1;
        renderPage();
    });
}

function renderPaymentStatus(status) {
    if (status === 'PAID') {
        return '<span class="status-badge status-completed">Đã trả</span>';
    } else {
        return '<span class="status-badge status-pending">Chờ trả</span>';
    }
}