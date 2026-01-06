import { showToast } from './toast.js';
import { fetchWithAuthCheck } from './api.js';

function initOrderPage() {
    console.log('DEBUG: Khởi tạo trang Orders.');

    const ORDER_API_URL = '/api/admin/orders';
    const ORDER_API_ALL_URL = '/api/admin/orders/all';
    const USER_API_URL = '/api/users';
    const SERVICE_API_URL = '/api/admin/services';
    const jwtToken = localStorage.getItem('jwtToken');

    const ordersTableBody = document.querySelector('#orders-table tbody');
    const orderModal = document.getElementById('order-modal');
    const customConfirmModal = document.getElementById('custom-confirm-modal');
    const orderForm = document.getElementById('order-form');
    const modalTitle = document.getElementById('modal-title');
    const addOrderBtn = document.getElementById('add-order-btn');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');
    const searchInput = document.getElementById('khachHangSearch');
    const suggestionList = document.getElementById('suggestion-list');
    const hiddenUserId = document.getElementById('khachHangId');
    const dichVuSearch = document.getElementById('dichVuSearch');
    const serviceSuggestionList = document.getElementById('service-suggestion-list');
    const dichVuIdInput = document.getElementById('dichVuId');
    const tongTienInput = document.getElementById('tongTien');
    const filterSearchInput = document.getElementById('order-search-input');
    const filterSortSelect = document.getElementById('order-sort-select');
    const filterStatus = document.getElementById('order-status-filter');
    const filterPayment = document.getElementById('order-payment-filter');

    let currentPage = 0;
    const pageSize = 4;
    let totalPages = 1;
    let confirmCallback = null;
    let cachedUsers = null;
    let cachedServices = null;
    let allUsers = [];
    let isLoadingAllUsers = false;
    let allServices = [];
    let isLoadingServices = false;
    let allOrders = []; // Chứa TẤT CẢ đơn hàng
    let filteredOrders = []; // Chứa đơn hàng sau khi lọc

    function isValidStatusTransition(current, next, isPaid) {
        if (current === next) {
        return true; // Cho phép lưu nếu trạng thái không thay đổi
    }
    const flow = {
        'CHUA_XU_LY': ['DA_NHAN', 'HUY'],
        'DA_NHAN': ['HOAN_THANH', 'HUY'],
        'HOAN_THANH': [], // Không được đổi gì nữa
        'HUY': []         // Không được đổi gì nữa
    };
    // Nếu đã thanh toán → không cho hủy
    if (isPaid && next === 'HUY') {
        showToast('Đơn hàng đã thanh toán không thể hủy!', 'error');
        return false;
    }
    return flow[current]?.includes(next) || false;
}

// Load toàn bộ user cho tìm kiếm
async function loadAllUsers() {
    if (allUsers.length > 0) return allUsers;
    if (isLoadingAllUsers) return allUsers; // Tránh gọi nhiều lần

    isLoadingAllUsers = true;
    const users = [];

    try {
        let page = 0;
        let totalPages = 1;

        do {
            const res = await fetch(`${USER_API_URL}?page=${page}&size=20`, {
                headers: { 'Authorization': `Bearer ${jwtToken}` }
            });

            if (!res.ok) throw new Error('Lỗi tải trang ' + page);

            const data = await res.json();
            const pageUsers = data.content || [];

            users.push(...pageUsers.map(u => ({
                id: u.id,
                hoTen: (u.hoTen || u.email || u.sdt || 'Khách vãng lai').trim(),
                sdt: u.sdt || 'Chưa có SĐT',
                search: removeDiacritics(
                    `${u.hoTen || ''} ${u.email || ''} ${u.sdt || ''}`.toLowerCase()
                )
            })));

            totalPages = data.totalPages || 1;
            page++;

        } while (page < totalPages);

        allUsers = users;
        console.log(`DEBUG: Đã tải ${allUsers.length} người dùng từ ${totalPages} trang`);
    } catch (e) {
        console.error('Lỗi loadAllUsers:', e);
        showToast('Không thể tải toàn bộ khách hàng', 'error');
    } finally {
        isLoadingAllUsers = false;
    }

    return allUsers;
}

function removeDiacritics(str) {
    if (!str) return '';
    return str
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')        // Xóa dấu
        .replace(/[^a-zA-Z0-9\s@._-]/g, '')     // GIỮ: chữ, số, @, ., _, -
        .trim();
}

function createSearchString(text) {
    if (!text) return '';
    return removeDiacritics(text) // (hàm removeDiacritics của bạn)
        .toLowerCase()
        .replace(/[0-9]/g, '') // Xóa số
        .replace(/\s+/g, ''); // Xóa khoảng trắng
}

// Tìm kiếm
searchInput?.addEventListener('input', async () => {
    const rawValue = searchInput.value.trim();
    const query = removeDiacritics(rawValue.toLowerCase());

    suggestionList.innerHTML = '';

    if (!rawValue) {
        suggestionList.style.display = 'none';
        hiddenUserId.value = '';
        return;
    }

    // ĐẢM BẢO ĐÃ LOAD DỮ LIỆU
    const users = await loadAllUsers();
    if (!users.length) {
        suggestionList.innerHTML = '<div class="suggestion-item">Không có dữ liệu</div>';
        suggestionList.style.display = 'block';
        return;
    }

    const matches = users
        .filter(u => u.search.includes(query))
        .slice(0, 10);

    if (!matches.length) {
        suggestionList.innerHTML = '<div class="suggestion-item">Không tìm thấy</div>';
        suggestionList.style.display = 'block';
        return;
    }

    matches.forEach(user => {
        const div = document.createElement('div');
        div.className = 'suggestion-item';
        div.innerHTML = `
            <div>
                <strong>${user.hoTen}</strong>
                ${user.sdt !== 'Chưa có SĐT' ? `<br><small style="color:#666;">${user.sdt}</small>` : ''}
            </div>
        `;
        div.onclick = () => {
            searchInput.value = user.hoTen;
            hiddenUserId.value = user.id;
            const sdtInput = document.getElementById('sdt');
            if (sdtInput) {
                if (user.sdt && user.sdt !== 'Chưa có SĐT') {
                sdtInput.value = user.sdt;
                sdtInput.disabled = true; // ĐÃ CÓ → KHÔNG CHO SỬA
                sdtInput.title = 'SĐT lấy từ hồ sơ khách hàng';
            } else {
                sdtInput.value = '';
                sdtInput.disabled = false; // KHÔNG CÓ → CHO NHẬP
                sdtInput.focus();
            }
        }
            suggestionList.style.display = 'none';
        };
        suggestionList.appendChild(div);
    });

    suggestionList.style.display = 'block';
});

document.addEventListener('click', (e) => {
    if (!searchInput?.contains(e.target) && !suggestionList?.contains(e.target)) {
        suggestionList.style.display = 'none';
    }
});

async function loadAllServices() {
    if (allServices.length > 0) return allServices;
    if (isLoadingServices) return allServices;

    isLoadingServices = true;
    const services = [];

    try {
        let page = 0;
        let totalPages = 1;

        do {
            const res = await fetch(`${SERVICE_API_URL}?page=${page}&size=20`, {
                headers: { 'Authorization': `Bearer ${jwtToken}` }
            });

            if (!res.ok) throw new Error('Lỗi tải dịch vụ trang ' + page);

            const data = await res.json();
            const pageServices = data.content || [];

            services.push(...pageServices.map(s => ({
                id: s.id,
                tenDichVu: s.tenDichVu || 'Không tên',
                giaCoBan: s.giaCoBan || 0,
                search: removeDiacritics((s.tenDichVu || '').toLowerCase())
            })));

            totalPages = data.totalPages || 1;
            page++;
        } while (page < totalPages);

        allServices = services;
        console.log(`DEBUG: Đã tải ${allServices.length} dịch vụ`);
    } catch (e) {
        console.error('Lỗi loadAllServices:', e);
        showToast('Không thể tải danh sách dịch vụ', 'error');
    } finally {
        isLoadingServices = false;
    }

    return allServices;
}

// Tìm kiếm dịch vụ
dichVuSearch?.addEventListener('input', async () => {
    const query = removeDiacritics(dichVuSearch.value.toLowerCase().trim());
    serviceSuggestionList.innerHTML = '';

    if (!query) {
        serviceSuggestionList.style.display = 'none';
        dichVuIdInput.value = '';
        tongTienInput.value = '';
        return;
    }

    const services = await loadAllServices();
    const matches = services
        .filter(s => s.search.includes(query))
        .slice(0, 10);

    if (!matches.length) {
        serviceSuggestionList.innerHTML = '<div class="suggestion-item">Không tìm thấy</div>';
        serviceSuggestionList.style.display = 'block';
        return;
    }

    matches.forEach(service => {
        const div = document.createElement('div');
        div.className = 'suggestion-item';
        div.innerHTML = `
            <span>${service.tenDichVu}</span>
            <span class="price">${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(service.giaCoBan)}</span>
        `;
        div.onclick = () => {
            dichVuSearch.value = service.tenDichVu;
            dichVuIdInput.value = service.id;
            tongTienInput.value = service.giaCoBan;
            serviceSuggestionList.style.display = 'none';
        };
        serviceSuggestionList.appendChild(div);
    });

    serviceSuggestionList.style.display = 'block';
});

// Đóng khi click ngoài
document.addEventListener('click', (e) => {
    if (!dichVuSearch?.contains(e.target) && !serviceSuggestionList?.contains(e.target)) {
        serviceSuggestionList.style.display = 'none';
    }
});

    // Kiểm tra sự tồn tại của các phần tử DOM
    if (!orderModal || !addOrderBtn || !ordersTableBody || !orderForm || !prevPageBtn || !nextPageBtn || !pageInfo) {
        console.error('LỖI CRITICAL: Thiếu phần tử DOM cần thiết.');
        return;
    }
    console.log('DEBUG: Các phần tử DOM cần thiết đã tồn tại.');

    // Hàm đóng tất cả modal
    function closeAllModals() {
        if (orderModal) orderModal.style.display = 'none';
        if (customConfirmModal) customConfirmModal.style.display = 'none';
        console.log('LOG: Đã đóng tất cả modal.');
    }

    function restoreTrangThaiOptions() {
    const trangThaiSelect = document.getElementById('trangThai');
    if (!trangThaiSelect) return;

    // XÓA TẤT CẢ
    trangThaiSelect.innerHTML = '';

    // KHÔI PHỤC 4 TRẠNG THÁI ĐẦY ĐỦ
    const fullStatuses = [
        { value: 'CHUA_XU_LY', text: 'Chưa Xử Lý' },
        { value: 'DA_NHAN', text: 'Đã Nhận' },
        { value: 'HOAN_THANH', text: 'Hoàn Thành' },
        { value: 'HUY', text: 'Hủy' }
    ];

    fullStatuses.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.value;
        opt.textContent = s.text;
        if (s.value === 'CHUA_XU_LY') opt.selected = true;
        trangThaiSelect.appendChild(opt);
    });

    trangThaiSelect.disabled = false;
}

function openAddOrderModal() {
    console.log('DEBUG: Mở modal thêm đơn hàng.');

    // 1. KHÔI PHỤC TRẠNG THÁI TRƯỚC
    restoreTrangThaiOptions();

    // 2. RESET FORM SAU ĐÓ → GIỮ LẠI OPTION
    orderForm.reset();
    document.getElementById('order-id').value = '';
    modalTitle.textContent = 'Thêm Đơn Hàng Mới';

    // --- MẶC ĐỊNH GIÁ TRỊ ---
// MẶC ĐỊNH: CHƯA XỬ LÝ + CHO PHÉP CHỌN PAID
    document.getElementById('trangThai').value = 'CHUA_XU_LY';
    const paymentSelect = document.getElementById('paymentStatus');
if (paymentSelect) {
    paymentSelect.value = 'UNPAID';
    paymentSelect.disabled = false; // ← BẮT BUỘC
    paymentSelect.classList.remove('locked');
    const lockMsg = paymentSelect.parentNode.querySelector('.lock-message');
    if (lockMsg) lockMsg.remove();
}

    // --- XÓA TÌM KIẾM & INPUT ---
    const searchInput = document.getElementById('khachHangSearch');
    const hiddenUserId = document.getElementById('khachHangId');
    const suggestionList = document.getElementById('suggestion-list');
    const sdtInput = document.getElementById('sdt');
    const notesInput = document.getElementById('notes');

    if (searchInput) searchInput.value = '';
    if (hiddenUserId) hiddenUserId.value = '';
    if (suggestionList) suggestionList.innerHTML = '';
    if (sdtInput) {
        sdtInput.value = '';
        sdtInput.disabled = false;
        sdtInput.readOnly = false;
    }
    if (notesInput) {
        notesInput.value = '';
        notesInput.readOnly = false;
    }

    // --- MỞ KHÓA TẤT CẢ TRƯỜNG ---
    const fieldsToEnable = [
        'khachHangSearch', 'dichVuSearch', 'thoiGianDat',
        'diaChiDichVu', 'tongTien', 'phuongThucThanhToan', 'trangThai', 'paymentStatus'
    ];
    fieldsToEnable.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.disabled = false;
            el.readOnly = false;
        }
    });

    setMinDateTime();
    orderForm.removeAttribute('data-current-status');

    // --- TẢI DỮ LIỆU + MỞ MODAL ---
    Promise.all([loadAllUsers(), loadAllServices()])
        .then(() => {
            setupPriceAutoFill();
            orderModal.style.display = 'flex';
            console.log('DEBUG: Modal đã mở thành công.');
        })
        .catch(error => {
            console.error('Lỗi mở modal:', error);
            showToast(`Không thể mở modal: ${error.message}`, 'error');
        });
}

function setupPriceAutoFill() {
    const serviceSelect = document.getElementById('dichVuId');
    const tongTienInput = document.getElementById('tongTien');

    const handler = () => {
        const price = serviceSelect.selectedOptions[0]?.dataset.price;
        tongTienInput.value = price ? parseFloat(price) : '';
        tongTienInput.readOnly = !!price;
    };

    serviceSelect.removeEventListener('change', handler);
    serviceSelect.addEventListener('change', handler);
}


    // Hàm render danh sách đơn hàng
    function renderOrders(orders, pageData) {
        if (!ordersTableBody) return;
        ordersTableBody.innerHTML = '';
        if (!Array.isArray(orders) || orders.length === 0) {
            ordersTableBody.innerHTML = `<tr><td colspan="11" style="text-align: center;">Chưa có đơn hàng nào.</td></tr>`;
            pageInfo.textContent = 'Trang 1 / 1';
            prevPageBtn.disabled = true;
            nextPageBtn.disabled = true;
            return;
        }

orders.forEach(order => {
    const row = ordersTableBody.insertRow();
    row.innerHTML = `
        <td>${order.id || 'N/A'}</td>
        <td>${escapeHtml(order.user?.hoTen || '—')}</td>
        <td>${escapeHtml(order.service?.tenDichVu || '—')}</td>

        <!-- Thời gian đặt -->
        <td class="text-center">
            <button class="description-icon view-detail" data-field="thoiGianDat" data-value="${order.thoiGianDat || ''}">
                <i class="fas fa-eye"></i>
            </button>
        </td>

        <!-- Địa chỉ -->
        <td class="text-center">
            <button class="description-icon view-detail" data-field="diaChiDichVu" data-value="${escapeHtml(order.diaChiDichVu || '')}">
                <i class="fas fa-eye"></i>
            </button>
        </td>

        <!-- Tổng tiền -->
        <td class="text-center">
            <button class="description-icon view-detail" data-field="tongTien" data-value="${order.tongTien || ''}">
                <i class="fas fa-eye"></i>
            </button>
        </td>

        <!-- Phương thức thanh toán -->
    <td class="text-center">
        <button class="description-icon view-detail" data-field="phuongThucThanhToan" data-value="${escapeHtml(order.phuongThucThanhToan || '')}">
            <i class="fas fa-eye"></i>
        </button>
    </td>

        <!-- Trạng thái -->
        <td><span class="status-badge status-${(order.trangThai || '').toLowerCase().replace(/_/g, '-')}">
            ${getStatusText(order.trangThai) || '—'}
        </span></td>

        <!-- SĐT -->
        <td>${escapeHtml(order.sdt || '—')}</td>

        <!-- Ghi chú -->
        <td class="text-center">
            <button class="description-icon view-detail" data-field="notes" data-value="${escapeHtml(order.notes || '')}">
                <i class="fas fa-eye"></i>
            </button>
        </td>

        <!-- Hành động -->
        <td>
            <button class="btn btn-info btn-sm edit-btn" data-id="${order.id}"><i class="fas fa-edit"></i></button>
        </td>
    `;
});

        totalPages = pageData.totalPages;
        pageInfo.textContent = `Trang ${currentPage + 1} / ${totalPages}`;
        prevPageBtn.disabled = currentPage === 0;
        nextPageBtn.disabled = currentPage >= totalPages - 1;
        console.log('DEBUG: Đã render bảng đơn hàng.');
    }

    // === HÀM AN TOÀN HTML (ngăn XSS) ===
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function fetchOrders() {
if (!jwtToken) {
 ordersTableBody.innerHTML = `<tr><td colspan="11" style="color: red; text-align: center;">Vui lòng đăng nhập.</td></tr>`;
 return;
}

// 1. THÊM HIỆU ỨNG LOADING TẠI ĐÂY
        ordersTableBody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align:center; padding: 20px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #007bff;"></i>
                    <div style="margin-top: 5px;">Đang tải dữ liệu...</div>
                </td>
            </tr>`;

        // Chỉ tải nếu cache rỗng
if (allOrders.length === 0) {
            console.log('DEBUG: Đang tải TẤT CẢ đơn hàng từ API...');
fetchWithAuthCheck(ORDER_API_ALL_URL, { // <-- Gọi API /all
 headers: { 'Authorization': `Bearer ${jwtToken}` }
 })
.then(response => response.json())
.then(data => {
                // API trả về List<Order>, không phải Page
allOrders = Array.isArray(data) ? data : [];
console.log(`DEBUG: Đã tải ${allOrders.length} đơn hàng.`);
 renderAndFilterOrders(0); // Gọi hàm render/filter chính
 })
.catch(error => {
 console.error('Lỗi API Đơn hàng:', error);
 ordersTableBody.innerHTML = `<tr><td colspan="11" style="color: red; text-align: center;">Lỗi: ${error.message}</td></tr>`;
showToast(error.message, 'error');
});
        } else {
            // Nếu đã có data, chỉ render lại
            renderAndFilterOrders(currentPage);
        }
}

    /**
     * Hàm MỚI: "Trái tim" xử lý Filter, Sort, Pagination
     */
    function renderAndFilterOrders(page = 0) {
        let filtered = [...allOrders];

        // 1. LẤY GIÁ TRỊ TỪ BỘ LỌC
        const query = createSearchString(filterSearchInput.value);
        const status = filterStatus.value;
        const payment = filterPayment.value;
        const sort = filterSortSelect.value;

        // 2. LỌC
        if (query) {
            filtered = filtered.filter(order => {
                const customerName = createSearchString(order.user?.hoTen);
                const customerPhone = createSearchString(order.sdt);
                const serviceName = createSearchString(order.service?.tenDichVu);
                
                return customerName.includes(query) || 
                       customerPhone.includes(query) || 
                       serviceName.includes(query);
            });
        }
        if (status) {
            filtered = filtered.filter(order => order.trangThai === status);
        }
        if (payment) {
            filtered = filtered.filter(order => order.phuongThucThanhToan === payment);
        }

        // 3. SẮP XẾP
        // Mặc định là mới nhất
        filtered.sort((a, b) => b.id - a.id); 

        if (sort === 'oldest') {
            filtered.sort((a, b) => a.id - b.id);
        } else if (sort === 'az') {
            filtered.sort((a, b) => (a.user?.hoTen || 'Z').localeCompare(b.user?.hoTen || 'Z'));
        } else if (sort === 'za') {
            filtered.sort((a, b) => (b.user?.hoTen || 'Z').localeCompare(a.user?.hoTen || 'Z'));
        }

        // 4. LƯU KẾT QUẢ VÀ PHÂN TRANG
        filteredOrders = filtered; // Lưu lại cho nút Next/Prev
        
        const totalItems = filteredOrders.length;
        const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
        
        currentPage = Math.min(page, totalPages - 1);
        if (page < 0) currentPage = 0;

        const start = currentPage * pageSize;
        const end = Math.min(start + pageSize, totalItems);
        const pageItems = filteredOrders.slice(start, end);

        // 5. RENDER BẢNG VÀ CẬP NHẬT UI
        renderOrdersTable(pageItems); // Gọi hàm render bảng
        updatePaginationUI(currentPage, totalPages); // Gọi hàm render phân trang
    }

    /**
     * Hàm MỚI: Chỉ để render BẢNG (tách từ hàm renderOrders cũ)
     */
function renderOrdersTable(orders) {
    if (!ordersTableBody) return;
    ordersTableBody.innerHTML = '';
    
    // TÍNH TOÁN LẠI SỐ CỘT (Colspan)
    // Bảng mới của bạn có 9 cột
    if (!Array.isArray(orders) || orders.length === 0) {
        ordersTableBody.innerHTML = `<tr><td colspan="9" style="text-align: center;">Không tìm thấy đơn hàng nào.</td></tr>`;
        return;
    }

    orders.forEach(order => {
        const row = ordersTableBody.insertRow();
        
        // Dữ liệu tên nhân viên (thêm mới)
        const employeeName = order.employee ? escapeHtml(order.employee.hoTen) : '<span style="color: #999;">Chưa gán</span>';

        row.innerHTML = `
            <td>${order.id || 'N/A'}</td>
            <td>${escapeHtml(order.user?.hoTen || '—')}</td>
            <td>${escapeHtml(order.service?.tenDichVu || '—')}</td>
            
            <td>${employeeName}</td> 
            
            <td class="text-center">
                <button class="description-icon view-detail" data-field="phuongThucThanhToan" data-value="${escapeHtml(order.phuongThucThanhToan || '')}">
                    <i class="fas fa-eye"></i>
                </button>
            </td>

            <td><span class="status-badge status-${(order.trangThai || '').toLowerCase().replace(/_/g, '-')}">
                ${getStatusText(order.trangThai) || '—'}
            </span></td>

            <td>${escapeHtml(order.sdt || '—')}</td>

            <td class="text-center">
                <button class="description-icon view-detail" data-field="notes" data-value="${escapeHtml(order.notes || '')}">
                    <i class="fas fa-eye"></i>
                </button>
            </td>

            <td>
                <button class="btn btn-info btn-sm edit-btn" data-id="${order.id}"><i class="fas fa-edit"></i></button>
            </td>
        `;
});
    }
    function updatePaginationUI(page, totalPages) {
pageInfo.textContent = `Trang ${page + 1} / ${totalPages}`;
prevPageBtn.disabled = page === 0;
nextPageBtn.disabled = page >= totalPages - 1;
    }

    // Hàm xử lý chỉnh sửa đơn hàng
function handleEdit(e) {
    const id = e.currentTarget.getAttribute('data-id');
    console.log('DEBUG: Sửa đơn hàng ID:', id);

    fetchWithAuthCheck(`${ORDER_API_URL}/${id}`, {
        headers: { 'Authorization': `Bearer ${jwtToken}` }
    })
    .then(response =>  response.json())
    .then(order => {
        // --- LẤY DOM ELEMENT VÀ KIỂM TRA NULL ---
        const orderIdInput = document.getElementById('order-id');
        const thoiGianDatInput = document.getElementById('thoiGianDat');
        const diaChiDichVuInput = document.getElementById('diaChiDichVu');
        const tongTienInput = document.getElementById('tongTien');
        const phuongThucThanhToanSelect = document.getElementById('phuongThucThanhToan');
        const trangThaiSelect = document.getElementById('trangThai');
        const modalTitle = document.getElementById('modal-title');
        const notesInput = document.getElementById('notes');
        const sdtInput = document.getElementById('sdt'); // ← CÓ THỂ NULL
        const paymentStatusSelect = document.getElementById('paymentStatus');
        const employeeNameInput = document.getElementById('employeeName');
        

        // --- KIỂM TRA DOM CẦN THIẾT ---
        if (!orderIdInput || !thoiGianDatInput || !diaChiDichVuInput || !tongTienInput ||
            !phuongThucThanhToanSelect || !trangThaiSelect || !modalTitle || !notesInput || !employeeNameInput) {
            console.error('LỖI DOM: Thiếu phần tử cần thiết trong modal');
            showToast('Lỗi hệ thống: Không tìm thấy trường nhập liệu.', 'error');
            return;
        }
if (paymentStatusSelect) {
    const isPaid = order.paymentStatus === 'PAID';
    
    // GÁN GIÁ TRỊ
    paymentStatusSelect.value = order.paymentStatus || 'UNPAID';

    // === KHÓA HOÀN TOÀN KHI ĐÃ PAID ===
    paymentStatusSelect.disabled = isPaid;

    // === GHI CHÚ TRẠNG THÁI BAN ĐẦU ===
    paymentStatusSelect.dataset.wasPaid = isPaid.toString();

    // === TÙY CHỌN: Thêm class CSS để UX rõ hơn ===
    if (isPaid) {
        paymentStatusSelect.classList.add('locked');
        // Tạo thông báo nhỏ bên dưới
        let lockMsg = paymentStatusSelect.parentNode.querySelector('.lock-message');
        if (!lockMsg) {
            lockMsg = document.createElement('small');
            lockMsg.className = 'lock-message text-muted';
            lockMsg.textContent = 'Đã thanh toán – Không thể thay đổi';
            lockMsg.style.display = 'block';
            lockMsg.style.marginTop = '4px';
            lockMsg.style.fontStyle = 'italic';
            paymentStatusSelect.parentNode.appendChild(lockMsg);
        }
    } else {
        const lockMsg = paymentStatusSelect.parentNode.querySelector('.lock-message');
        if (lockMsg) lockMsg.remove();
        paymentStatusSelect.classList.remove('locked');
    }

    // === BẮT BUỘC GỬI DÙ DISABLED ===
    // → Dùng hidden input để đảm bảo gửi
    let hiddenPayment = document.getElementById('hidden-payment-status');
    if (!hiddenPayment) {
        hiddenPayment = document.createElement('input');
        hiddenPayment.type = 'hidden';
        hiddenPayment.id = 'hidden-payment-status';
        hiddenPayment.name = 'paymentStatus';
        orderForm.appendChild(hiddenPayment);
    }
    hiddenPayment.value = paymentStatusSelect.value;
    // LƯU ĐỂ DÙNG TRONG SUBMIT
    orderForm.paymentStatusSelect = paymentStatusSelect;
    paymentStatusSelect.removeEventListener('change', updateHiddenPayment);
    // Gắn listener mới
    paymentStatusSelect.addEventListener('change', updateHiddenPayment);

    function updateHiddenPayment() {
        if (hiddenPayment) {
            // Cập nhật input ẩn mỗi khi select thay đổi
            hiddenPayment.value = paymentStatusSelect.value;
            console.log('DEBUG: Giá trị input ẩn đã cập nhật =', hiddenPayment.value);
        }
    }
}

        // --- GÁN GIÁ TRỊ AN TOÀN ---
        orderIdInput.value = order.id;
        thoiGianDatInput.value = order.thoiGianDat ? formatLocalDateTime(order.thoiGianDat) : '';
        diaChiDichVuInput.value = order.diaChiDichVu || '';
        tongTienInput.value = order.tongTien ? order.tongTien.toString() : '';
        phuongThucThanhToanSelect.value = order.phuongThucThanhToan || '';
        trangThaiSelect.value = order.trangThai || 'CHUA_XU_LY';
        modalTitle.textContent = `Sửa Đơn Hàng: #${order.id}`;
        notesInput.value = order.notes || '';
        notesInput.readOnly = true;

        orderForm.dataset.currentStatus = order.trangThai;

        // --- GÁN SDT NẾU CÓ INPUT ---
        if (sdtInput) {
            sdtInput.value = order.sdt || '';
            sdtInput.disabled = true; // Admin không sửa
        }

        if (order.employee) {
            employeeNameInput.value = `${order.employee.hoTen} (ID: ${order.employee.id})`;
        } else {
            employeeNameInput.value = 'Chưa có nhân viên nào nhận đơn';
        }

        // --- TẢI USER & SERVICE ĐỂ HIỂN THỊ TÊN ---
        Promise.all([loadAllUsers(), loadAllServices()])
            .then(([users, services]) => {
                const user = users.find(u => u.id == order.user?.id);
                if (user) {
                    const khachHangSearch = document.getElementById('khachHangSearch');
                    const khachHangId = document.getElementById('khachHangId');
                    if (khachHangSearch && khachHangId) {
                        khachHangSearch.value = user.hoTen;
                        khachHangId.value = user.id;
                        khachHangSearch.disabled = true;
                    }
                }

                const service = services.find(s => s.id == order.service?.id);
                if (service) {
                    const dichVuSearch = document.getElementById('dichVuSearch');
                    const dichVuId = document.getElementById('dichVuId');
                    if (dichVuSearch && dichVuId) {
                        dichVuSearch.value = service.tenDichVu;
                        dichVuId.value = service.id;
                        tongTienInput.value = service.giaCoBan;
                        dichVuSearch.disabled = true;
                    }
                }

                // --- KHÓA CÁC TRƯỜNG ---
                thoiGianDatInput.disabled = true;
                diaChiDichVuInput.disabled = true;
                tongTienInput.readOnly = true;
                phuongThucThanhToanSelect.disabled = true;

                // --- CẬP NHẬT TRẠNG THÁI (có kiểm soát luồng) ---
const currentStatus = order.trangThai;
trangThaiSelect.dataset.currentStatus = currentStatus;

const validNextStatuses = {
    'CHUA_XU_LY': ['DA_NHAN', 'HUY'],
    'DA_NHAN': ['HOAN_THANH', 'HUY'],
    'HOAN_THANH': [],
    'HUY': []
};

trangThaiSelect.innerHTML = '';

// 1. Thêm trạng thái hiện tại
const currentOpt = document.createElement('option');
currentOpt.value = currentStatus;
currentOpt.textContent = getStatusText(currentStatus);
currentOpt.selected = true;
trangThaiSelect.appendChild(currentOpt);

// 2. Thêm các trạng thái tiếp theo
const possibleStatuses = validNextStatuses[currentStatus] || [];
possibleStatuses.forEach(status => {
    const opt = document.createElement('option');
    opt.value = status;
    opt.textContent = getStatusText(status);
    trangThaiSelect.appendChild(opt);
});

trangThaiSelect.disabled = possibleStatuses.length === 0;

if (possibleStatuses.length === 0) {
    // showToast('Đơn hàng đã hoàn tất hoặc hủy, không thể thay đổi trạng thái.', 'info'); // <-- BỎ DÒNG NÀY

    // === THÊM TIN NHẮN TĨNH BÊN DƯỚI ===
    let lockMsg = trangThaiSelect.parentNode.querySelector('.status-lock-message');
    if (!lockMsg) {
        lockMsg = document.createElement('small');
        lockMsg.className = 'status-lock-message text-muted'; // Dùng class khác
        lockMsg.textContent = 'Đã hoàn tất/hủy, không thể thay đổi.';
        lockMsg.style.display = 'block';
        lockMsg.style.marginTop = '4px';
        lockMsg.style.fontStyle = 'italic';
        trangThaiSelect.parentNode.appendChild(lockMsg);
    }
} else {
    // Xóa tin nhắn nếu có (khi mở đơn hàng còn xử lý được)
    const lockMsg = trangThaiSelect.parentNode.querySelector('.status-lock-message');
    if (lockMsg) lockMsg.remove();
    
    trangThaiSelect.disabled = false;
}

                orderModal.style.display = 'flex';
                console.log('LOG: Modal sửa đơn hàng đã mở thành công.');
            })
            .catch(error => {
                console.error('Lỗi tải user/service:', error);
                showToast('Không thể tải thông tin khách hàng/dịch vụ.', 'error');
            });
    })
    .catch(error => {
        console.error('Lỗi tải đơn hàng:', error);
        showToast(error.message || 'Lỗi không xác định', 'error');
    });
}

// --- HÀM HỖ TRỢ HIỂN THỊ TÊN TRẠNG THÁI ---
function getStatusText(status) {
    const map = {
        'CHUA_XU_LY': 'Chưa Xử Lý',
        'DA_NHAN': 'Đã Nhận',
        'HOAN_THANH': 'Hoàn Thành',
        'HUY': 'Hủy'
    };
    return map[status] || status;
}

    function setMinDateTime() {
    const thoiGianDatInput = document.getElementById('thoiGianDat');
    if (!thoiGianDatInput) return;

    // Lấy ngày hiện tại + 5 ngày
    const now = new Date();
    const minDate = new Date(now.getTime() + 5 * 24 * 60 * 60 * 1000);

    // Format: YYYY-MM-DDTHH:mm
    const year = minDate.getFullYear();
    const month = String(minDate.getMonth() + 1).padStart(2, '0');
    const day = String(minDate.getDate()).padStart(2, '0');
    const hours = String(minDate.getHours()).padStart(2, '0');
    const minutes = String(minDate.getMinutes()).padStart(2, '0');

    const minDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;
    thoiGianDatInput.min = minDateTime;

    console.log('DEBUG: Đã đặt min cho thời gian đặt:', minDateTime);
}

function formatLocalDateTime(isoString) {
    if (!isoString) return '';
    return isoString.substring(0, 16); 
}

    // Hàm hiển thị modal xác nhận
    function showCustomConfirm(message, callback) {
        const confirmMessage = document.getElementById('confirm-message');
        if (confirmMessage) {
            confirmMessage.textContent = message;
            confirmCallback = callback;
            customConfirmModal.style.display = 'flex';
            console.log('LOG: Modal xác nhận đã được mở.');
        } else {
            console.error('LỖI: Thiếu phần tử #confirm-message.');
        }
    }

    // Sự kiện nút thêm đơn hàng
    addOrderBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        console.log('LOG CLICK: Nút Thêm Đơn Hàng Mới được kích hoạt.');
        openAddOrderModal();
    });

    // Sự kiện đóng modal
    document.querySelectorAll('.close-btn').forEach(btn => {
        btn.addEventListener('click', closeAllModals);
    });

    // Sự kiện click ngoài modal để đóng
    window.addEventListener('click', (event) => {
        if (event.target === orderModal || event.target === customConfirmModal) {
            closeAllModals();
        }
    });

    // Sự kiện xử lý click trên bảng đơn hàng
    document.removeEventListener('click', handleOrderClick);
    function handleOrderClick(event) {
        if (!document.querySelector('#orders-table')) {
            console.log('DEBUG: Bỏ qua handleOrderClick vì không ở trang orders.');
            return;
        }
        const target = event.target.closest('button');
        if (!target) return;

        if (target.classList.contains('edit-btn')) {
            event.preventDefault();
            console.log('LOG CLICK: Nút Sửa đơn hàng được bắt.');
            handleEdit({ currentTarget: target });
        } else if (target.classList.contains('delete-btn')) {
            event.preventDefault();
            console.log('LOG CLICK: Nút Xóa đơn hàng được bắt.');
            handleDelete({ currentTarget: target });
        }
    }
    document.addEventListener('click', handleOrderClick);

if (orderForm) {
    orderForm.addEventListener('submit', (e) => {
        e.preventDefault();
        console.log('DEBUG: Submit form đơn hàng.');

        const id = document.getElementById('order-id')?.value;
        const method = id ? 'PUT' : 'POST';
        const url = id ? `${ORDER_API_URL}/${id}` : ORDER_API_URL;

        // === LẤY DỮ LIỆU ===
        const userId = hiddenUserId.value;
        const serviceId = dichVuIdInput.value;
        const thoiGianDat = document.getElementById('thoiGianDat')?.value;
        const diaChiDichVu = document.getElementById('diaChiDichVu')?.value.trim();
        const tongTien = parseFloat(document.getElementById('tongTien')?.value);
        const phuongThucThanhToan = document.getElementById('phuongThucThanhToan')?.value;
        const trangThai = document.getElementById('trangThai').value;
        const notes = document.getElementById('notes')?.value.trim();
        const sdt = document.getElementById('sdt')?.value.trim();
        const paymentStatus = document.getElementById('hidden-payment-status')?.value || document.getElementById('paymentStatus').value || 'UNPAID';
        const paymentStatusSelect = orderForm.paymentStatusSelect;
        const wasPaid = paymentStatusSelect?.dataset.wasPaid === 'true';
        // === LẤY currentStatus TỪ DATASET ===
        const currentStatus = orderForm.dataset.currentStatus;

        // === VALIDATE CƠ BẢN ===
        if (!userId) return showToast('Vui lòng chọn khách hàng.', 'error');
        if (!serviceId) return showToast('Vui lòng chọn dịch vụ.', 'error');
        if (!thoiGianDat) return showToast('Thời gian đặt không được để trống.', 'error');
        if (!diaChiDichVu) return showToast('Địa chỉ dịch vụ không được để trống.', 'error');
        if (isNaN(tongTien) || tongTien <= 0) return showToast('Tổng tiền phải là số dương.', 'error');
        if (!phuongThucThanhToan) return showToast('Vui lòng chọn phương thức thanh toán.', 'error');
        if (!trangThai) return showToast('Vui lòng chọn trạng thái.', 'error');
        if (!sdt) return showToast('Vui lòng nhập số điện thoại.', 'error');
        if (!/^0[3|5|7|8|9][0-9]{8}$/.test(sdt)) {
            return showToast('SĐT không hợp lệ. Phải có 10 số, bắt đầu bằng 03, 05, 07, 08, 09.', 'error');
        }

// Dự phòng: nếu ai đó bypass disabled
if (wasPaid && paymentStatus === 'UNPAID') {
    showToast('Không thể chuyển từ ĐÃ THANH TOÁN về CHƯA THANH TOÁN!', 'error');
    return;
}
        // === KIỂM TRA TRẠNG THÁI (CHỈ KHI SỬA) ===
        if (id && currentStatus) {
            const isPaid = paymentStatus === 'PAID';
            if (!isValidStatusTransition(currentStatus, trangThai, isPaid)) {
                return;
            }
        }

        // === CHẶN: PAID → HUY (luôn kiểm tra, kể cả thêm mới) ===
        if (paymentStatus === 'PAID' && trangThai === 'HUY') {
            showToast('Đơn hàng đã thanh toán không thể hủy!', 'error');
            return;
        }

        // === TẠO OBJECT GỬI LÊN SERVER ===
        const order = {
            userId: parseInt(userId),
            serviceId: parseInt(serviceId),
            thoiGianDat,
            diaChiDichVu,
            tongTien,
            phuongThucThanhToan,
            trangThai,
            notes,
            sdt,
            paymentStatus // ← LUÔN GỬI
        };

        console.log('DEBUG: Gửi dữ liệu:', order);

        // === GỬI REQUEST ===
        fetch(url, {
            method,
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(order)
        })
        .then(async response => {
            if (!response.ok) {
                const text = await response.text();
                console.error('Server error:', text);
                throw new Error('Lỗi server: ' + (text || response.status));
            }
            
        })
        .then(() => {
            showToast(`Đơn hàng đã được ${method === 'POST' ? 'thêm' : 'cập nhật'} thành công!`, 'success');
            closeAllModals();
            allOrders = []; // Xóa cache
            fetchOrders(); // Tải lại TẤT CẢ
        })
        .catch(error => {
            console.error('Lỗi khi lưu đơn hàng:', error);
            showToast(error.message || 'Lỗi không xác định', 'error');
        });
    });
}

// Thêm vào cuối initOrderPage()
document.addEventListener('click', (e) => {
    const btn = e.target.closest('.view-detail');
    if (!btn) return;

    const field = btn.dataset.field;
    const value = btn.dataset.value;

    const titleMap = {
        thoiGianDat: 'Thời Gian Đặt',
        diaChiDichVu: 'Địa Chỉ Dịch Vụ',
        tongTien: 'Tổng Tiền',
        notes: 'Ghi Chú'
    };

    let displayValue = '';
    if (field === 'thoiGianDat' && value) {
        displayValue = new Date(value).toLocaleString('vi-VN', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit'
        });
    } else if (field === 'tongTien' && value) {
        displayValue = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
    } else if (field === 'notes') {
        displayValue = value || 'Không có ghi chú';
    } else {
        displayValue = value || '—';
    }

    document.getElementById('detail-title').textContent = titleMap[field] || 'Chi Tiết';
    document.getElementById('detail-content').textContent = displayValue;

    document.getElementById('detail-modal').style.display = 'flex';
});

// Đóng modal chi tiết
document.querySelectorAll('[data-close-modal="detail-modal"]').forEach(btn => {
    btn.addEventListener('click', () => {
        document.getElementById('detail-modal').style.display = 'none';
    });
});

    // Sự kiện nút xác nhận xóa
    const confirmYesBtn = document.getElementById('confirm-yes-btn');
    const confirmNoBtn = document.getElementById('confirm-no-btn');
    if (confirmYesBtn) {
        confirmYesBtn.addEventListener('click', () => {
            customConfirmModal.style.display = 'none';
            if (confirmCallback) confirmCallback(true);
            console.log('LOG: Xác nhận - Đồng ý.');
        });
    }
    if (confirmNoBtn) {
        confirmNoBtn.addEventListener('click', () => {
            customConfirmModal.style.display = 'none';
            if (confirmCallback) confirmCallback(false);
            console.log('LOG: Xác nhận - Hủy bỏ.');
        });
    }

// 1. Gán sự kiện cho Filter/Sort (MỚI)
    filterSearchInput.addEventListener('input', () => renderAndFilterOrders(0));
    filterSortSelect.addEventListener('change', () => renderAndFilterOrders(0));
    filterStatus.addEventListener('change', () => renderAndFilterOrders(0));
    filterPayment.addEventListener('change', () => renderAndFilterOrders(0));

    // 2. Gán sự kiện Phân trang (SỬA LẠI)
 prevPageBtn.addEventListener('click', () => {
 if (currentPage > 0) {
 renderAndFilterOrders(currentPage - 1); // Gọi hàm filter chính
 }
 });
 nextPageBtn.addEventListener('click', () => {
 const totalPages = Math.ceil(filteredOrders.length / pageSize); // Lấy từ mảng đã lọc
 if (currentPage < totalPages - 1) {
 renderAndFilterOrders(currentPage + 1); // Gọi hàm filter chính
}
 });

    // Khởi tạo trang bằng cách lấy danh sách đơn hàng
    fetchOrders();
    console.log('DEBUG: initOrderPage hoàn tất.');
    
}

export { initOrderPage };

