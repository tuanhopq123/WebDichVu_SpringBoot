import { fetchWithAuthCheck } from './auth.js';
import { showToast } from './toast.js';
import { formatTimeAgo, showOrderDetails } from './utils.js';

let allHistory = [];
let currentPage = 1;
const pageSize = 10;
let currentSearchTerm = "";
let currentStatusFilter = "";
let currentSort = "newest";

function normalizeString(str) {
    if (!str) return '';
    return str
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "");
}

export async function loadMyHistory() {
    console.log("Đang tải trang Lịch sử...");
    const tableBody = document.getElementById('history-table-body');
    tableBody.innerHTML = '<tr><td colspan="6"><p>Đang tải lịch sử của bạn...</p></td></tr>';

    setupFilterListeners();

    try {
        const response = await fetchWithAuthCheck('/api/assignments/my-history', {
            method: 'GET'
        });
        if (!response.ok) throw new Error('Không thể tải lịch sử.');

        allHistory = await response.json();
        renderPage();

    } catch (error) {
        console.error('Lỗi tải lịch sử:', error);
        tableBody.innerHTML = '<tr><td colspan="6"><p>Lỗi khi tải dữ liệu. Vui lòng thử lại.</p></td></tr>';
    }
}

function renderPage() {
    // 1. Lọc (Filter)
    const normalizedSearch = normalizeString(currentSearchTerm);
    let filteredHistory = allHistory;

    // Lọc theo Status
    if (currentStatusFilter) {
        filteredHistory = filteredHistory.filter(oa => {
            const orderStatus = oa.order.trangThai;
            const assignStatus = oa.status;

            switch (currentStatusFilter) {
                // "Đã nhận (Đang làm)" = Đã chấp nhận NHƯNG đơn hàng chưa Hoàn thành
                case 'ACCEPTED':
                    return assignStatus === 'ACCEPTED' && orderStatus !== 'HOAN_THANH' && orderStatus !== 'HUY';
                // "Đã hoàn thành" = Trạng thái đơn hàng là HOAN_THANH
                case 'COMPLETED':
                    return orderStatus === 'HOAN_THANH';
                case 'REJECTED':
                    return assignStatus === 'REJECTED';
                case 'PENDING':
                    return assignStatus === 'PENDING';
                default:
                    return true;
            }
        });
    }

    // Lọc theo Tìm kiếm
    if (normalizedSearch) {
        filteredHistory = filteredHistory.filter(oa => {
            const order = oa.order;
            const customer = order.user;
            const service = order.service;
            
            return (
                normalizeString(customer.hoTen).includes(normalizedSearch) ||
                normalizeString(customer.email).includes(normalizedSearch) ||
                normalizeString(service.tenDichVu).includes(normalizedSearch) ||
                normalizeString(order.sdt).includes(normalizedSearch)
            );
        });
    }

    // 2. Sắp xếp (Sort)
    filteredHistory.sort((a, b) => {
        const customerA = normalizeString(a.order.user.hoTen);
        const customerB = normalizeString(b.order.user.hoTen);
        const dateA = new Date(a.assignedAt);
        const dateB = new Date(b.assignedAt);

        switch (currentSort) {
            case 'az':
                return customerA.localeCompare(customerB);
            case 'za':
                return customerB.localeCompare(customerA);
            case 'oldest':
                return dateA - dateB;
            case 'newest':
            default:
                return dateB - dateA;
        }
    });

    // 3. Phân trang (Paginate)
    const totalItems = filteredHistory.length;
    const totalPages = Math.ceil(totalItems / pageSize);
    if (currentPage > totalPages) currentPage = totalPages;
    if (currentPage < 1) currentPage = 1;

    const startIndex = (currentPage - 1) * pageSize;
    const paginatedItems = filteredHistory.slice(startIndex, startIndex + pageSize);

    // 4. Vẽ (Render)
    renderHistoryTable(paginatedItems);
    renderPagination(totalPages, totalItems);
}

function renderHistoryTable(historyItems) {
    const tableBody = document.getElementById('history-table-body');
    tableBody.innerHTML = ""; // Xóa dữ liệu cũ

    if (historyItems.length === 0) {
        // Cập nhật colspan="6" (vì vẫn 6 cột)
        tableBody.innerHTML = '<tr><td colspan="6"><p>Không tìm thấy dữ liệu nào khớp.</p></td></tr>';
        return;
    }

    historyItems.forEach(oa => {
        const order = oa.order;
        const row = document.createElement('tr');
        
        // SỬA LẠI NỘI DUNG DÒNG
        row.innerHTML = `
            <td>${order.service.tenDichVu}</td>
            <td>${order.user.hoTen}</td>
            <td>${order.sdt || '(Không có)'}</td>
            <td>${renderAssignmentStatus(oa.status)}</td>
            <td>${renderOrderStatus(order.trangThai)}</td>
            <td>
                <button class="btn btn-sm btn-secondary btn-view-details">
                    <i class="fas fa-eye"></i> Xem
                </button>
            </td>
        `;

        row.querySelector('.btn-view-details').addEventListener('click', () => {
            showOrderDetails(order);
        });

        tableBody.appendChild(row);
    });
}

function renderPagination(totalPages, totalItems) {
    const container = document.getElementById('history-pagination');
    container.innerHTML = ""; 

    if (totalItems === 0) return;

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

function setupFilterListeners() {
    document.getElementById('history-search-input').addEventListener('input', (e) => {
        currentSearchTerm = e.target.value;
        currentPage = 1;
        renderPage();
    });

    document.getElementById('history-status-filter').addEventListener('change', (e) => {
        currentStatusFilter = e.target.value;
        currentPage = 1;
        renderPage();
    });

    document.getElementById('history-sort-filter').addEventListener('change', (e) => {
        currentSort = e.target.value;
        currentPage = 1;
        renderPage();
    });
}

function renderAssignmentStatus(status) {
    switch (status) {
        case 'PENDING':
            return '<span class="status-badge status-pending">Đang chờ</span>';
        case 'ACCEPTED':
            return '<span class="status-badge status-accepted">Đã chấp nhận</span>';
        case 'REJECTED':
            return '<span class="status-badge status-rejected">Đã từ chối</span>';
        default:
            return `<span class="status-badge">${status}</span>`;
    }
}

function renderOrderStatus(status) {
    switch (status) {
        case 'CHUA_XU_LY':
            return '<span class="status-badge status-pending">Chờ xử lý</span>';
        case 'DA_NHAN':
            return '<span class="status-badge status-accepted">Đang thực hiện</span>';
        case 'HOAN_THANH':
            return '<span class="status-badge status-completed">Hoàn thành</span>';
        case 'HUY':
            return '<span class="status-badge status-cancelled">Đã hủy</span>';
        default:
            return `<span class="status-badge">${status}</span>`;
    }
}