import { fetchWithAuthCheck } from './api.js';
import { showToast } from './toast.js';

// === KHAI BÁO BIẾN TRẠNG THÁI (STATE) MỚI ===
let allPendingOrders = [];  // Lưu trữ danh sách gốc từ API
let currentPage = 1;
const pageSize = 5;        // 10 đơn hàng mỗi trang
let currentSort = 'default';// 'default', 'az', 'za'
let currentSearchTerm = ''; // Nội dung tìm kiếm

let currentOrderToInvite = null; 

// === MAIN FUNCTION (KHỞI CHẠY) ===
export async function loadAssignmentsPage() {
    console.log("Đang tải trang Giao đơn...");
    
    // Thiết lập trình nghe sự kiện cho modal (giữ nguyên)
    setupModalEventListeners();

    // Thiết lập trình nghe sự kiện MỚI cho tìm kiếm và sắp xếp
    setupFilterListeners();

    // Tải dữ liệu từ API và hiển thị lần đầu
    await fetchAndStoreAssignments();
}

/**
 * MỚI: Hàm chuẩn hóa chuỗi để tìm kiếm
 * (Xóa dấu, chuyển chữ thường, bỏ số, bỏ khoảng trắng)
 */
function normalizeString(str) {
    if (!str) return '';
    return str
        .toLowerCase()
        .normalize("NFD") // Tách dấu
        .replace(/[\u0300-\u036f]/g, "") // Xóa dấu
        // .replace(/\d+/g, '') // Bỏ số (tùy chọn)
        .replace(/\s+/g, ' '); // Chuẩn hóa khoảng trắng
}

/**
 * MỚI: Thiết lập trình nghe sự kiện cho Search và Sort
 */
function setupFilterListeners() {
    // Live search
    const searchInput = document.getElementById('assignments-search-input');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            currentSearchTerm = e.target.value;
            currentPage = 1; // Reset về trang 1 khi tìm kiếm
            renderPage(); // Vẽ lại trang
        });
    }

    // Sort
    const sortSelect = document.getElementById('assignments-sort-select');
    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            currentSort = e.target.value;
            currentPage = 1; // Reset về trang 1 khi sắp xếp
            renderPage(); // Vẽ lại trang
        });
    }
}

/**
 * CẬP NHẬT: Đổi tên thành 'fetchAndStoreAssignments'
 * Chỉ tải dữ liệu 1 LẦN và lưu vào biến 'allPendingOrders'
 */
async function fetchAndStoreAssignments() {
    const tableBody = document.getElementById('assignments-table-body');
    if (!tableBody) return;
    tableBody.innerHTML = `
        <tr>
            <td colspan="7" style="text-align:center; padding: 30px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 30px; color: #007bff;"></i>
                <div style="margin-top: 10px; font-weight: 500; color: #666;">Đang tải dữ liệu đơn hàng...</div>
            </td>
        </tr>
    `;

    try {
        const response = await fetchWithAuthCheck('/api/admin/orders/all?status=CHUA_XU_LY', {
            method: 'GET'
        });
        const allOrders = await response.json();
        
        allPendingOrders = allOrders.filter(order => order.trangThai === 'CHUA_XU_LY');
        
        // Sau khi tải xong, VẼ TRANG ĐẦU TIÊN
        renderPage();

    } catch (error) {
        console.error('Lỗi khi tải đơn hàng:', error);
        tableBody.innerHTML = '<tr><td colspan="7">Lỗi khi tải dữ liệu.</td></tr>';
        allPendingOrders = []; // Đảm bảo mảng rỗng nếu lỗi
        renderPage(); // Vẫn gọi renderPage để xử lý UI
    }
}

/**
 * MỚI: Hàm TRUNG TÂM xử lý (lọc, sắp xếp, phân trang)
 * Hàm này sẽ được gọi mỗi khi tìm kiếm, sắp xếp, hoặc chuyển trang
 */
function renderPage() {
    // 1. Lọc (Filter)
    const normalizedSearch = normalizeString(currentSearchTerm);
    let filteredOrders = allPendingOrders;

    if (normalizedSearch) {
        filteredOrders = allPendingOrders.filter(order => {
            const customerName = normalizeString(order.user.hoTen);
            const serviceName = normalizeString(order.service.tenDichVu);
            return customerName.includes(normalizedSearch) || serviceName.includes(normalizedSearch);
        });
    }

    // 2. Sắp xếp (Sort)
    if (currentSort === 'az') {
        filteredOrders.sort((a, b) => 
            normalizeString(a.user.hoTen).localeCompare(normalizeString(b.user.hoTen))
        );
    } else if (currentSort === 'za') {
        filteredOrders.sort((a, b) => 
            normalizeString(b.user.hoTen).localeCompare(normalizeString(a.user.hoTen))
        );
    }
    // 'default' = giữ nguyên thứ tự từ API

    // 3. Phân trang (Paginate)
    const totalItems = filteredOrders.length;
    const totalPages = Math.ceil(totalItems / pageSize);

    // Đảm bảo trang hiện tại hợp lệ
    if (currentPage > totalPages && totalPages > 0) {
        currentPage = totalPages;
    }
    if (currentPage < 1) {
        currentPage = 1;
    }

    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const paginatedOrders = filteredOrders.slice(startIndex, endIndex);

    // 4. Vẽ lại Bảng và Thanh phân trang
    renderAssignmentsTable(paginatedOrders);
    renderPagination(totalPages, totalItems);
}


/**
 * CẬP NHẬT: Hàm 'renderAssignmentsTable'
 * Chỉ vẽ các đơn hàng được đưa cho (đã phân trang)
 */
function renderAssignmentsTable(orders) {
    const tableBody = document.getElementById('assignments-table-body');
    tableBody.innerHTML = ''; // Xóa nội dung cũ

    // Cập nhật thông báo nếu không có dữ liệu
    if (orders.length === 0) {
        if (currentSearchTerm) {
            tableBody.innerHTML = `<tr><td colspan="7">Không tìm thấy đơn hàng nào khớp với "${currentSearchTerm}".</td></tr>`;
        } else {
            tableBody.innerHTML = '<tr><td colspan="7">Không có đơn hàng nào cần giao.</td></tr>';
        }
        return;
    }

    orders.forEach(order => {
        
        // === PHẦN ĐỊNH NGHĨA BIẾN BẠN BỊ THIẾU LÀ ĐÂY ===
        const requiredCount = order.soLuong;
        const acceptedCount = order.assignments 
            ? order.assignments.filter(a => a.status === 'ACCEPTED').length 
            : 0;
        const acceptedNames = order.assignments
            ? order.assignments
                .filter(a => a.status === 'ACCEPTED')
                .map(a => a.employee ? a.employee.hoTen : 'N/A')
                .join(', ')
            : 'Chưa có';
        // =============================================
            
        const row = `
            <tr>
                <td>${order.id}</td>
                <td>${order.user.hoTen}</td>
                <td>${order.service.tenDichVu}</td>
                <td>${new Date(order.thoiGianDat).toLocaleString('vi-VN')}</td>
                <td>
                    <span class="status-badge ${acceptedCount === requiredCount ? 'status-completed' : 'status-pending'}">
                        ${acceptedCount} / ${requiredCount}
                    </span>
                </td>
                <td>${acceptedNames || 'Chưa có'}</td>
                <td>
                    <button class="btn btn-sm btn-action" 
                            data-order-id="${order.id}" 
                            data-required-count="${order.soLuong}" 
                            data-service-name="${order.service.tenDichVu}"
                            data-service-id="${order.service.id}">
                        <i class="fas fa-user-plus"></i> Mời
                    </button>
                </td>
            </tr>
        `;
        tableBody.insertAdjacentHTML('beforeend', row);
    });

    // Gán sự kiện click cho các nút "Mời" (Giữ nguyên)
    // Phải chạy lại mỗi lần render bảng
    document.querySelectorAll('.btn-action[data-order-id]').forEach(button => {
        button.addEventListener('click', handleOpenInviteModal);
    });
}

/**
 * MỚI: Hàm vẽ thanh phân trang
 */
function renderPagination(totalPages, totalItems) {
    const paginationContainer = document.getElementById('assignments-pagination');
    if (!paginationContainer) return;

    // Áp dụng style từ ví dụ của bạn
    paginationContainer.style.marginTop = '20px';
    paginationContainer.style.textAlign = 'center';

    // 1. Nếu không có gì
    if (totalItems === 0) {
        paginationContainer.innerHTML = '<span id="page-info">Không có đơn hàng nào</span>';
        return;
    }

    const isFirstPage = (currentPage === 1);
    const isLastPage = (currentPage === totalPages);

 // 2. Tạo HTML
paginationContainer.innerHTML = `
    <button class="btn btn-secondary"
            id="prev-page-btn"
            style="padding: 4px 10px; font-size: 15px;"
            ${isFirstPage ? 'disabled' : ''}>
        Trước
    </button>

    <span id="page-info"
          style="margin: 0 10px; font-size: 17px;">
        Trang ${currentPage} / ${totalPages}
    </span>

    <button class="btn btn-secondary"
            id="next-page-btn"
            style="padding: 4px 10px; font-size: 15px;"
            ${isLastPage ? 'disabled' : ''}>
        Sau
    </button>
`;

    // 3. Gán sự kiện bằng ID (thay vì data-page)

    // Nút Trang Trước
    const prevBtn = paginationContainer.querySelector('#prev-page-btn');
    if (prevBtn) {
        prevBtn.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage--;
                renderPage(); // Vẽ lại trang
            }
        });
    }

    // Nút Trang Sau
    const nextBtn = paginationContainer.querySelector('#next-page-btn');
    if (nextBtn) {
        nextBtn.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                renderPage(); // Vẽ lại trang
            }
        });
    }
}


// ===============================================
// === CÁC HÀM MODAL (GIỮ NGUYÊN) ===
// ===============================================

async function handleOpenInviteModal(event) {
    const button = event.currentTarget;
    const orderId = button.dataset.orderId;
    const requiredCount = button.dataset.requiredCount;
    const serviceName = button.dataset.serviceName;

    currentOrderToInvite = orderId; 

    document.getElementById('modal-order-id').textContent = orderId;
    document.getElementById('modal-required-count').textContent = requiredCount;
    document.getElementById('modal-service-name').textContent = serviceName;
    const serviceId = button.dataset.serviceId;

    const listContainer = document.getElementById('available-employees-list');
    listContainer.innerHTML = '<p>Đang tải danh sách nhân viên rảnh...</p>';

    document.getElementById('invite-modal').style.display = 'flex';

    try {
        const response = await fetchWithAuthCheck(
            `/api/users/employees/available?serviceId=${serviceId}`, 
            { method: 'GET' }
        );
        const employees = await response.json(); 

        if (employees.length === 0) {
            listContainer.innerHTML = '<p>Không có nhân viên nào đang rảnh.</p>';
            return;
        }

        listContainer.innerHTML = '';
        employees.forEach(emp => {
            const checkboxItem = `
                <div class="checkbox-item">
                    <input type="checkbox" id="emp-${emp.id}" name="employeeInvite" value="${emp.id}">
                    <label for="emp-${emp.id}">${emp.hoTen} (Email: ${emp.email})</label>
                </div>
            `;
            listContainer.insertAdjacentHTML('beforeend', checkboxItem);
        });

    } catch (error) {
        console.error('Lỗi khi tải nhân viên rảnh:', error);
        listContainer.innerHTML = '<p>Lỗi khi tải danh sách nhân viên.</p>';
    }
}

function closeModal() {
    document.getElementById('invite-modal').style.display = 'none';
    currentOrderToInvite = null;
    document.getElementById('available-employees-list').innerHTML = '';
}

function setupModalEventListeners() {
    document.getElementById('modal-close-btn').addEventListener('click', closeModal);
    document.getElementById('modal-cancel-btn').addEventListener('click', closeModal);
    document.getElementById('modal-submit-invite-btn').addEventListener('click', handleSubmitInvites);
}

async function handleSubmitInvites() {
    if (!currentOrderToInvite) return;

    const selectedCheckboxes = document.querySelectorAll('input[name="employeeInvite"]:checked');
    const employeeIds = Array.from(selectedCheckboxes).map(cb => cb.value);

    if (employeeIds.length === 0) {
        showToast('Vui lòng chọn ít nhất một nhân viên.', 'error');
        return;
    }

    const button = document.getElementById('modal-submit-invite-btn');
    button.disabled = true;
    button.textContent = 'Đang gửi...';

    try {
        const requestBody = {
            employeeIds: employeeIds
        };

        await fetchWithAuthCheck(`/api/admin/orders/${currentOrderToInvite}/invite`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody) 
        });

        showToast(`Đã gửi ${employeeIds.length} lời mời thành công.`, 'success');
        closeModal();
        
        // CẬP NHẬT: Không cần fetch lại toàn bộ, chỉ cần cập nhật
        // bản ghi trong 'allPendingOrders' và gọi renderPage()
        // (Cách đơn giản: Tải lại toàn bộ)
        await fetchAndStoreAssignments(); 

    } catch (error) {
        console.error('Lỗi khi gửi lời mời:', error);
        let errorMsg = 'Lỗi khi gửi lời mời.';
        try {
            const errorData = await error.json(); // Thử parse lỗi JSON
            errorMsg = errorData.error || errorMsg;
        } catch(e) {
            // Không parse được, dùng lỗi mặc định
        }
        showToast(errorMsg, 'error');
    } finally {
        button.disabled = false;
        button.textContent = 'Gửi lời mời';
    }
}