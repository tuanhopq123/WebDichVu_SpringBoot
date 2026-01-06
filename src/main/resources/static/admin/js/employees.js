import { showToast } from './toast.js';
import { fetchWithAuthCheck } from './api.js';
// Chúng ta sẽ định nghĩa một hàm tìm kiếm tùy chỉnh, vì vậy không cần import 'createSearchString'

const EMPLOYEE_API_URL = '/api/users/employees';
const SERVICE_API_URL = '/api/admin/services/all';

// --- BIẾN TOÀN CỤC MỚI ---
let allEmployeesCache = []; // Lưu trữ TẤT CẢ nhân viên từ API
let filteredEmployees = []; // Lưu trữ danh sách đã được lọc/sắp xếp
let currentPage = 0;
const PAGE_SIZE = 5; // 5 nhân viên mỗi trang
// -------------------------

let allServicesCache = [];
let currentAssignedServiceId = null; 

function createEmployeeSearchString(text) {
    if (!text) return '';
    return text
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '') // Xóa dấu
        .toLowerCase(); // Giữ lại số, chỉ chuyển chữ thường
}

function initEmployeePage() {
    console.log('DEBUG: Khởi tạo trang Employees.');
    const employeeTableBody = document.querySelector('#employees-table tbody');
    const employeeModal = document.getElementById('employee-modal');
    
    // --- BỘ CHỌN MỚI CHO BỘ LỌC VÀ PHÂN TRANG ---
    const searchInput = document.getElementById('employee-search-input');
    const sortSelect = document.getElementById('employee-sort-select');
    const statusFilter = document.getElementById('employee-status-filter');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');
    
    if (!employeeTableBody || !employeeModal || !searchInput || !sortSelect || !statusFilter || !prevPageBtn || !nextPageBtn) {
        console.error("LỖI: Thiếu các thành phần DOM (bộ lọc, phân trang) của trang employees.");
        return;
    }

    loadEmployees(employeeTableBody); // Tải dữ liệu 1 lần
    
    // SỬA LỖI: Truyền đúng biến 'employeeTableBody'
    setupModal(employeeModal, employeeTableBody);
    
    loadAllServices(); // Tải dịch vụ cho modal

    // --- SỰ KIỆN MỚI CHO BỘ LỌC VÀ PHÂN TRANG ---
    searchInput.addEventListener('input', () => {
        renderFilteredEmployees(0); // Lọc và quay về trang 0
    });
    sortSelect.addEventListener('change', () => {
        renderFilteredEmployees(0); // Sắp xếp và quay về trang 0
    });
    statusFilter.addEventListener('change', () => {
        renderFilteredEmployees(0); // Lọc và quay về trang 0
    });

    prevPageBtn.addEventListener('click', () => {
        if (currentPage > 0) {
            renderFilteredEmployees(currentPage - 1);
        }
    });
    nextPageBtn.addEventListener('click', () => {
        const totalPages = Math.ceil(filteredEmployees.length / PAGE_SIZE);
        if (currentPage < totalPages - 1) {
            renderFilteredEmployees(currentPage + 1);
        }
    });
}

async function loadEmployees(tableBody) {
    try {
            tableBody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align:center; padding: 20px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #007bff;"></i>
                    <div style="margin-top: 5px;">Đang tải dữ liệu...</div>
                </td>
            </tr>`;
        allEmployeesCache = []; // Xóa cache trước khi tải
        const response = await fetchWithAuthCheck(EMPLOYEE_API_URL);
        allEmployeesCache = await response.json();
        console.log(`DEBUG: Đã tải ${allEmployeesCache.length} nhân viên vào cache.`);
        renderFilteredEmployees(0); // Hiển thị trang đầu tiên
    } catch (error) {
        console.error('Lỗi tải danh sách nhân viên:', error);
        showToast(error.message, 'error');
        if(tableBody) tableBody.innerHTML = `<tr><td colspan="7" class="text-center error-message">${error.message}</td></tr>`;
    }
}

function renderFilteredEmployees(page = 0) {
    const searchInput = document.getElementById('employee-search-input');
    const sortSelect = document.getElementById('employee-sort-select');
    const statusFilter = document.getElementById('employee-status-filter');
    const tableBody = document.querySelector('#employees-table tbody');

    if (!tableBody) return; // Thoát nếu table không tồn tại

    let tempEmployees = [...allEmployeesCache];
    const query = createEmployeeSearchString(searchInput.value);
    const status = statusFilter.value;
    const sort = sortSelect.value;

    // 1. LỌC (TÌM KIẾM)
    if (query) {
        tempEmployees = tempEmployees.filter(emp => {
            const name = createEmployeeSearchString(emp.hoTen);
            const email = createEmployeeSearchString(emp.email);
            const phone = createEmployeeSearchString(emp.sdt); // sdt có thể là null
            return name.includes(query) || email.includes(query) || (phone && phone.includes(query));
        });
    }

    // 2. LỌC (TRẠNG THÁI)
    if (status) {
        tempEmployees = tempEmployees.filter(emp => emp.trangThaiLamViec === status);
    }

    // 3. SẮP XẾP
    tempEmployees.sort((a, b) => {
        const nameA = a.hoTen || '';
        const nameB = b.hoTen || '';
        if (sort === 'az') {
            return nameA.localeCompare(nameB);
        } else { // 'za'
            return nameB.localeCompare(nameA);
        }
    });

    // 4. LƯU KẾT QUẢ VÀ TRANG HIỆN TẠI
    filteredEmployees = tempEmployees;
    currentPage = page;

    // 5. PHÂN TRANG
    const totalPages = Math.max(1, Math.ceil(filteredEmployees.length / PAGE_SIZE));
    currentPage = Math.min(page, totalPages - 1); // Đảm bảo trang hợp lệ
    
    const start = currentPage * PAGE_SIZE;
    const end = start + PAGE_SIZE;
    const pageItems = filteredEmployees.slice(start, end);

    // 6. RENDER
    renderEmployeeTable(pageItems, tableBody);
    updatePaginationUI(currentPage, totalPages);
}

/**
 * Chỉ hiển thị nhân viên ra bảng (Không chứa logic)
 */
function renderEmployeeTable(employees, tableBody) {
    tableBody.innerHTML = '';
    if (!employees || employees.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="7" class="text-center">Không tìm thấy nhân viên nào.</td></tr>';
        return;
    }

    employees.forEach(emp => {
        const row = tableBody.insertRow();
        
        const servicesHtml = emp.dichVuDamNhan && emp.dichVuDamNhan.length > 0
            ? emp.dichVuDamNhan.map(s => `<span class="service-pill">${s.tenDichVu}</span>`).join('')
            : '<em>Chưa gán</em>';

        row.innerHTML = `
            <td>${emp.id}</td>
            <td>${emp.hoTen}</td>
            <td>${emp.email}</td>
            <td>${emp.sdt || '—'}</td>
            <td>${getStatusBadge(emp.trangThaiLamViec)}</td>
            <td class="service-cell">${servicesHtml}</td>
            <td>
                <button class="btn btn-info btn-sm edit-employee-btn" data-id="${emp.id}">
                    <i class="fas fa-edit"></i> Sửa
                </button>
            </td>
        `;
    });
}

function updatePaginationUI(page, totalPages) {
    const pageInfo = document.getElementById('page-info');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');

    if (pageInfo && prevPageBtn && nextPageBtn) {
        pageInfo.textContent = `Trang ${page + 1} / ${totalPages}`;
        prevPageBtn.disabled = (page === 0);
        nextPageBtn.disabled = (page >= totalPages - 1);
    }
}

function getStatusBadge(status) {
    if (status === 'BAN') {
        return '<span class="badge badge-danger">Bận</span>';
    }
    if (status === 'RANH') {
        return '<span class="badge badge-success">Rảnh</span>';
    }
    return '<span class="badge badge-secondary">Chưa rõ</span>';
}

async function loadAllServices() {
    if (allServicesCache.length > 0) return;
    try {
        const response = await fetchWithAuthCheck(SERVICE_API_URL);
        allServicesCache = await response.json();
        console.log(`DEBUG: Đã tải ${allServicesCache.length} dịch vụ vào cache.`);
    } catch (error) {
        console.error('Lỗi tải danh sách dịch vụ:', error);
        showToast('Không thể tải danh sách dịch vụ cho tìm kiếm.', 'error');
    }
}

function setupModal(modal, tableBody) {
    const form = document.getElementById('employee-form');
    const serviceSearchInput = document.getElementById('service-search-input');
    const serviceSearchResults = document.getElementById('service-search-results');
    const assignedServicesList = document.getElementById('assigned-services-list');

    document.addEventListener('click', (e) => {
        const editBtn = e.target.closest('.edit-employee-btn');
        if (editBtn && document.querySelector('#employees-table')) {
            const id = editBtn.dataset.id;
            openEditModal(id, modal);
        }
    });

    serviceSearchInput.addEventListener('input', () => {
        if (currentAssignedServiceId !== null) {
            serviceSearchResults.innerHTML = '';
            return;
        }

        const query = createEmployeeSearchString(serviceSearchInput.value);
        if (!query) {
            serviceSearchResults.innerHTML = '';
            return;
        }
        
        const filtered = allServicesCache.filter(service => {
            if (currentAssignedServiceId === service.id) { 
                return false;
            }
            const serviceName = createEmployeeSearchString(service.tenDichVu);
            return serviceName.includes(query);
        }).slice(0, 5); 

        renderServiceSearchResults(filtered, serviceSearchResults);
    });

    serviceSearchResults.addEventListener('click', (e) => {
        const resultItem = e.target.closest('.service-result-item');
        if (resultItem) {
            const id = parseInt(resultItem.dataset.id, 10);
            currentAssignedServiceId = id;
            renderAssignedServices(assignedServicesList);
            serviceSearchInput.value = '';
            serviceSearchResults.innerHTML = '';
        }
    });

    assignedServicesList.addEventListener('click', (e) => {
        const assignedItem = e.target.closest('.assigned-service-item');
        if (assignedItem) {
            currentAssignedServiceId = null; 
            renderAssignedServices(assignedServicesList);
        }
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('employee-id').value;
        const sdt = document.getElementById('employee-sdt').value;
        const status = document.getElementById('employee-status').value;
        
        const payload = {
            sdt: sdt,
            trangThaiLamViec: status,
            assignedServiceId: currentAssignedServiceId 
        };

        try {
            const response = await fetchWithAuthCheck(`${EMPLOYEE_API_URL}/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                showToast('Cập nhật nhân viên thành công!', 'success');
                modal.style.display = 'none';
                
                // CẬP NHẬT QUAN TRỌNG: Xóa cache và tải lại
                allEmployeesCache = []; 
                loadEmployees(tableBody);
            } else {
                 const error = await response.json();
                throw new Error(error.message || 'Cập nhật thất bại.');
            }
        } catch (error) {
            console.error('Lỗi submit form nhân viên:', error);
            showToast(error.message, 'error');
        }
    });
}

async function openEditModal(id, modal) {
    try {
        const response = await fetchWithAuthCheck(`${EMPLOYEE_API_URL}/${id}`);
        const emp = await response.json();

        document.getElementById('employee-modal-title').textContent = `Sửa Nhân Viên: ${emp.hoTen}`;
        document.getElementById('employee-id').value = emp.id;
        document.getElementById('employee-name').value = emp.hoTen;
        document.getElementById('employee-email').value = emp.email;
        document.getElementById('employee-sdt').value = emp.sdt || '';
        document.getElementById('employee-status').value = emp.trangThaiLamViec || 'RANH';

        currentAssignedServiceId = null; 
        if (emp.dichVuDamNhan && emp.dichVuDamNhan.length > 0) {
            currentAssignedServiceId = emp.dichVuDamNhan[0].id;
        }
        
        renderAssignedServices(document.getElementById('assigned-services-list'));

        document.getElementById('service-search-input').value = '';
        document.getElementById('service-search-results').innerHTML = '';

        modal.style.display = 'flex';
    } catch (error) {
        console.error('Lỗi mở modal sửa:', error);
        showToast('Không thể tải chi tiết nhân viên.', 'error');
    }
    document.addEventListener('click', (e) => {
    if (!e.target.closest('#service-search-input') && !e.target.closest('#service-search-results')) {
        document.getElementById('service-search-results').innerHTML = '';
    }
});
        // Đóng modal khi click vào nút ×
document.querySelectorAll('.close-btn').forEach(btn => {
    btn.onclick = () => {
        const modalId = btn.getAttribute('data-close-modal');
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'none';
        }
    };
});
}

function renderServiceSearchResults(services, container) {
    container.innerHTML = '';
    if (services.length === 0) {
        container.innerHTML = '<div class="service-result-item disabled">Không tìm thấy...</div>';
        return;
    }
    services.forEach(service => {
        container.innerHTML += `
            <div class="service-result-item" data-id="${service.id}">
                <strong>${service.tenDichVu}</strong> (ID: ${service.id})
            </div>
        `;
    });
}

function renderAssignedServices(container) {
    const searchInput = document.getElementById('service-search-input');
    container.innerHTML = '';

    if (currentAssignedServiceId === null) {
        container.innerHTML = '<em>Chưa gán dịch vụ nào.</em>';
        if (searchInput) {
            searchInput.disabled = false;
            searchInput.placeholder = "Gõ để tìm dịch vụ...";
        }
        return;
    }

    const service = allServicesCache.find(s => s.id === currentAssignedServiceId);
    if (service) {
        container.innerHTML += `
            <span class="assigned-service-item" data-id="${service.id}">
                ${service.tenDichVu}
                <i class="fas fa-times-circle remove-icon"></i>
            </span>
        `;
    }
    
    if (searchInput) {
        searchInput.disabled = true;
        searchInput.placeholder = "Xóa dịch vụ hiện tại để thêm mới";
    }

}

export { initEmployeePage };

