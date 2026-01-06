import { showToast } from './toast.js';
import { fetchWithAuthCheck } from './api.js';
function initUserPage() {
    console.log('DEBUG: Khởi tạo trang Users.');

    const USER_API_URL = '/api/users';
    const jwtToken = localStorage.getItem('jwtToken');

    const usersTableBody = document.querySelector('#users-table tbody');
    const userModal = document.getElementById('user-modal');
    const customConfirmModal = document.getElementById('custom-confirm-modal');
    const userForm = document.getElementById('user-form');
    const modalTitle = document.getElementById('modal-title');
    const addUserBtn = document.getElementById('add-user-btn');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');
    const filterSearchInput = document.getElementById('user-search-input');
    const filterSortSelect = document.getElementById('user-sort-select');
    const filterRole = document.getElementById('user-role-filter');

    let currentPage = 0;
    const pageSize = 5;
    let totalPages = 1;
    let allUsers = []; // Chứa TẤT CẢ user
    let filteredUsers = []; // Chứa user đã lọc
    let confirmCallback = null;

    if (!userModal || !addUserBtn || !usersTableBody || !userForm || !prevPageBtn || !nextPageBtn || !pageInfo) {
        console.error('LỖI CRITICAL: Thiếu phần tử DOM cần thiết.');
        showToast('Lỗi giao diện: Thiếu phần tử cần thiết.', 'error');
        return;
    }
    console.log('DEBUG: Các phần tử DOM cần thiết đã tồn tại.');

    // === HÀM HỖ TRỢ TÌM KIẾM (MỚI) ===
function removeDiacritics(str) {
    if (!str) return '';
        return str
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '') 
        .replace(/[^a-zA-Z0-9\s@._-]/g, '')
        .trim();
}

 function createSearchString(text) {
 if (!text) return '';
return removeDiacritics(text)
 .toLowerCase() // không phân biệt hoa/thường
.replace(/[0-9]/g, '') // xóa số
 .replace(/\s+/g, ''); // xóa khoảng trắng
 }

    function openAddUserModal() {
        console.log('DEBUG: Hàm openAddUserModal được gọi.');
        userModal.style.display = 'flex';
        userModal.style.zIndex = '1001';
        userModal.style.visibility = 'visible';
        if (userForm) {
            userForm.reset();
            document.getElementById('user-id').value = '';
            document.getElementById('matKhau').required = true; // Bắt buộc mật khẩu khi thêm mới
            modalTitle.textContent = 'Thêm Người Dùng Mới';
        }
        console.log('LOG MODAL: Đã mở modal thêm người dùng.');
    }

    function closeAllModals() {
        if (userModal) userModal.style.display = 'none';
        if (customConfirmModal) customConfirmModal.style.display = 'none';
        console.log('LOG: Đã đóng tất cả modal.');
    }

    addUserBtn.removeEventListener('click', openAddUserModal);
    addUserBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        console.log('LOG CLICK: Nút Thêm Người Dùng Mới được kích hoạt.');
        openAddUserModal();
    });

    document.querySelectorAll('.close-btn').forEach(btn => {
        btn.removeEventListener('click', closeAllModals);
        btn.addEventListener('click', closeAllModals);
    });

    window.removeEventListener('click', handleOutsideClick);
    function handleOutsideClick(event) {
        if (event.target === userModal || event.target === customConfirmModal) {
            closeAllModals();
        }
    }
    window.addEventListener('click', handleOutsideClick);

    function renderUsers(users, pageData) {
        if (!usersTableBody) return;
        usersTableBody.innerHTML = '';
        if (!Array.isArray(users) || users.length === 0) {
            usersTableBody.innerHTML = `<tr><td colspan="5" style="text-align: center;">Chưa có người dùng nào.</td></tr>`;
            pageInfo.textContent = 'Trang 1 / 1';
            prevPageBtn.disabled = true;
            nextPageBtn.disabled = true;
            console.log('DEBUG: Không có người dùng để render.');
            return;
        }

        const getRoleBadge = (role) => {
            if (role === 'ADMIN') return '<span class="badge badge-danger">Admin</span>';
            return '<span class="badge badge-success">User</span>';
        };

        users.forEach(user => {
            const row = usersTableBody.insertRow();
            row.innerHTML = `
                <td>${user.id || 'N/A'}</td>
                <td>${user.hoTen || '—'}</td>
                <td>${user.email || '—'}</td>
                <td>${getRoleBadge(user.vaiTro)}</td>
                <td>
                    <button class="btn btn-info btn-sm edit-btn" data-id="${user.id}"><i class="fas fa-edit"></i></button>
                    <button class="btn btn-danger btn-sm delete-btn" data-id="${user.id}"><i class="fas fa-trash"></i></button>
                </td>
            `;
        });

        totalPages = pageData.totalPages;
        pageInfo.textContent = `Trang ${currentPage + 1} / ${totalPages}`;
        prevPageBtn.disabled = currentPage === 0;
        nextPageBtn.disabled = currentPage >= totalPages - 1;
        console.log('DEBUG: Đã render bảng người dùng.');
    }

    function handleEdit(e) {
        const id = e.currentTarget.getAttribute('data-id');
        console.log('DEBUG: Sửa ID:', id, 'Token:', jwtToken);
        fetchWithAuthCheck(`${USER_API_URL}/${id}`, {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        })
            .then(response => response.json())
            .then(user => {
                const userIdInput = document.getElementById('user-id');
                const hoTenInput = document.getElementById('hoTen');
                const emailInput = document.getElementById('email');
                const matKhauInput = document.getElementById('matKhau');
                const vaiTroInput = document.getElementById('vaiTro');
                if (!userIdInput || !hoTenInput || !emailInput || !matKhauInput || !vaiTroInput) {
                    console.error('LỖI: Thiếu phần tử DOM trong handleEdit.');
                    showToast('Không tìm thấy các trường cần thiết.', 'error');
                    return;
                }
                userIdInput.value = user.id;
                hoTenInput.value = user.hoTen || '';
                emailInput.value = user.email || '';
                matKhauInput.value = ''; // Để trống mật khẩu khi chỉnh sửa
                matKhauInput.required = false; // Không bắt buộc mật khẩu khi chỉnh sửa
                vaiTroInput.value = user.vaiTro || 'KHACH';
                modalTitle.textContent = `Sửa Người Dùng: ${user.hoTen}`;
                userModal.style.display = 'flex';
                userModal.style.zIndex = '1001';
                console.log('LOG MODAL: Modal Sửa Người Dùng đã được mở.');
            })
            .catch(error => {
                console.error('Lỗi tải dữ liệu sửa:', error);
                showToast(`Không thể tải dữ liệu người dùng: ${error.message}`, 'error');
            });
    }

    if (userForm) {
        userForm.removeEventListener('submit', handleSubmit);
        userForm.addEventListener('submit', handleSubmit);
        function handleSubmit(e) {
            e.preventDefault();

            const id = document.getElementById('user-id').value;
            const method = id ? 'PUT' : 'POST';
            const url = id ? `${USER_API_URL}/${id}` : USER_API_URL;

            const hoTen = document.getElementById('hoTen').value.trim();
            const email = document.getElementById('email').value.trim();
            const matKhau = document.getElementById('matKhau').value.trim();
            const vaiTro = document.getElementById('vaiTro').value;

            if (!hoTen) {
                showToast('Họ tên không được để trống.', 'error');
                return;
            }
            if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                showToast('Email không hợp lệ.', 'error');
                return;
            }
            if (!id && !matKhau) {
                showToast('Mật khẩu không được để trống khi thêm người dùng mới.', 'error');
                return;
            }
            if (matKhau && matKhau.length < 6) {
                showToast('Mật khẩu phải có ít nhất 6 ký tự.', 'error');
                return;
            }

            const params = new URLSearchParams();
            params.append('hoTen', hoTen);
            params.append('email', email);
            params.append('vaiTro', vaiTro);
            if (matKhau) {
                params.append('matKhau', matKhau);
            }

            fetch(url, {
                method: method,
                headers: { 'Authorization': `Bearer ${jwtToken}` },
                body: params
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => {
                            if (response.status === 400 && err.error === 'Email đã tồn tại.') {
                                throw new Error('Email đã tồn tại.');
                            }
                            throw new Error(err.error || `Lỗi ${response.status}: Vui lòng kiểm tra dữ liệu đầu vào.`);
                        }).catch(() => {
                            throw new Error('Lỗi xử lý yêu cầu.');
                        });
                    }
                    return response.json();
                })
                .then(() => {
                    showToast(`Người dùng đã được ${method === 'POST' ? 'thêm' : 'cập nhật'} thành công!${matKhau ? ' Mật khẩu đã được cập nhật.' : ''}`, 'success');
                    closeAllModals();
                    //fetchUsers(currentPage);
                    allUsers = []; // Xóa cache
                    fetchUsers(); // Tải lại TẤT CẢ
                })
                .catch(error => {
                    console.error('Lỗi khi lưu người dùng:', error);
                    showToast(error.message, 'error');
                });
        }
    }

    function showCustomConfirm(message, callback) {
        const confirmMessage = document.getElementById('confirm-message');
        if (confirmMessage) {
            confirmMessage.textContent = message;
            confirmCallback = callback;
            customConfirmModal.style.display = 'flex';
            console.log('LOG: Modal xác nhận xóa đã được mở.');
        } else {
            console.error('LỖI: Thiếu phần tử #confirm-message.');
        }
    }

    document.getElementById('confirm-yes-btn')?.removeEventListener('click', confirmYes);
    document.getElementById('confirm-yes-btn')?.addEventListener('click', confirmYes);
    function confirmYes() {
        customConfirmModal.style.display = 'none';
        if (confirmCallback) confirmCallback(true);
        console.log('LOG: Xác nhận xóa - Đồng ý.');
    }

    document.getElementById('confirm-no-btn')?.removeEventListener('click', confirmNo);
    document.getElementById('confirm-no-btn')?.addEventListener('click', confirmNo);
    function confirmNo() {
        customConfirmModal.style.display = 'none';
        if (confirmCallback) confirmCallback(false);
        console.log('LOG: Xác nhận xóa - Hủy bỏ.');
    }

function fetchUsers() {
if (!jwtToken) {
 usersTableBody.innerHTML = `<tr><td colspan="6" style="color: red; text-align: center;">Vui lòng đăng nhập.</td></tr>`;
return;
 }
// 1. THÊM HIỆU ỨNG LOADING TẠI ĐÂY
        usersTableBody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align:center; padding: 20px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #007bff;"></i>
                    <div style="margin-top: 5px;">Đang tải dữ liệu...</div>
                </td>
            </tr>`;
 // Chỉ tải nếu cache rỗng
if (allUsers.length === 0) {
 console.log('DEBUG: Đang tải TẤT CẢ user từ API /all...');
 fetch(`${USER_API_URL}/all`, { // <-- GỌI API MỚI
 headers: { 'Authorization': `Bearer ${jwtToken}` }
 })
 .then(response => {
 if (response.status === 403) throw new Error('Bạn không có quyền truy cập.');
if (!response.ok) throw new Error(`Lỗi tải user: ${response.status}`);
 return response.json();
})
 .then(data => {
allUsers = Array.isArray(data) ? data : [];
 console.log(`DEBUG: Đã tải ${allUsers.length} user.`);
renderAndFilterUsers(0); // Render lần đầu
 })
 .catch(error => {
 console.error('Lỗi API User:', error);
 usersTableBody.innerHTML = `<tr><td colspan="6" style="color: red; text-align: center;">Lỗi: ${error.message}</td></tr>`;
 showToast(error.message, 'error');
 });
 } else {
 renderAndFilterUsers(currentPage); // Render lại từ cache
 }
 }

function renderAndFilterUsers(page = 0) {
 let filtered = [...allUsers];

 // 1. LẤY GIÁ TRỊ TỪ BỘ LỌC
 const query = createSearchString(filterSearchInput.value);
const role = filterRole.value;
 const sort = filterSortSelect.value;

 // 2. LỌC
 if (query) {
filtered = filtered.filter(user => {
 const hoTen = createSearchString(user.hoTen);
 const email = createSearchString(user.email);
 return hoTen.includes(query) || email.includes(query);
 });
}
 if (role) {
 filtered = filtered.filter(user => user.vaiTro === role);
 }

// 3. SẮP XẾP
 if (sort === 'az') {
 filtered.sort((a, b) => (a.hoTen || 'Z').localeCompare(b.hoTen || 'Z'));
 } else if (sort === 'za') {
 filtered.sort((a, b) => (b.hoTen || 'Z').localeCompare(a.hoTen || 'Z'));
 }

 // 4. LƯU KẾT QUẢ VÀ PHÂN TRANG
 filteredUsers = filtered;
 const totalItems = filteredUsers.length;
 const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
 currentPage = Math.min(page, totalPages - 1);
 if (page < 0) currentPage = 0;
 
 const start = currentPage * pageSize;
const end = Math.min(start + pageSize, totalItems);
const pageItems = filteredUsers.slice(start, end);

// 5. RENDER
 renderUsersTable(pageItems);
updatePaginationUI(currentPage, totalPages);
}

function renderUsersTable(users) {
usersTableBody.innerHTML = '';
if (!Array.isArray(users) || users.length === 0) {
 // Phải là colspan="6" vì HTML của bạn có 6 cột
usersTableBody.innerHTML = `<tr><td colspan="6" style="text-align: center;">Không tìm thấy người dùng nào.</td></tr>`;
 return;
}

const getRoleBadge = (role) => {
 if (role === 'ADMIN') return '<span class="badge badge-danger">Admin</span>';
 if (role === 'NHAN_VIEN') return '<span class="badge badge-info">Nhân Viên</span>';
 return '<span class="badge badge-success">User</span>';
 };

 const getStatusBadge = (enabled) => {
if (enabled === true) return '<span class="badge badge-success">Hoạt động</span>';
return '<span class="badge badge-secondary">Đã khóa</span>';
 };

 users.forEach(user => {
 const row = usersTableBody.insertRow();
const isEnabled = user.enabled; 
 
 const toggleButton = `
 <button 
class="btn btn-warning btn-sm toggle-status-btn" 
data-id="${user.id}" 
data-enabled="${isEnabled}" 
 title="${isEnabled ? 'Khóa người dùng' : 'Mở khóa người dùng'}">
 <i class="fas ${isEnabled ? 'fa-lock' : 'fa-lock-open'}"></i>
</button>
`;

// === CODE ĐÃ SỬA LỖI HTML ===

row.innerHTML = `
    <td>${user.id || 'N/A'}</td>
    <td>${user.hoTen || '—'}</td>
    <td>${user.email || '—'}</td>
    <td>${getRoleBadge(user.vaiTro)}</td>
    
    <td>${getStatusBadge(isEnabled)}</td> 
    
    <td>
        <button class="btn btn-info btn-sm edit-btn" data-id="${user.id}"><i class="fas fa-edit"></i></button>
        ${toggleButton}
        <button class="btn btn-danger btn-sm delete-btn" data-id="${user.id}"><i class="fas fa-trash"></i></button>
    </td>
`;
});
}

 /**
     * HÀM MỚI: Chỉ cập nhật UI phân trang
     */
function updatePaginationUI(page, totalPages) {
 pageInfo.textContent = `Trang ${page + 1} / ${totalPages}`;
 prevPageBtn.disabled = page === 0;
 nextPageBtn.disabled = page >= totalPages - 1;
}
    function handleDelete(e) {
        const idToDelete = e.currentTarget.getAttribute('data-id');
        showCustomConfirm(`Bạn có chắc chắn muốn xóa Người Dùng ID: ${idToDelete} không? Hành động này không thể hoàn tác.`, (confirmed) => {
            if (confirmed) {
                fetch(`${USER_API_URL}/${idToDelete}`, {
                    method: 'DELETE',
                    headers: { 'Authorization': `Bearer ${jwtToken}` }
                })
                    .then(response => {
                        if (response.status === 204 || response.status === 200) {
                            showToast('Người dùng đã được xóa thành công.', 'success');
                            fetchUsers(currentPage);
                        } else if (response.status === 404) {
                            showToast('Không tìm thấy người dùng để xóa.', 'error');
                        } else {
                            return response.json().then(err => {
                                throw new Error(err.error || `Lỗi ${response.status}: Xóa không thành công.`);
                            });
                        }
                    })
                    .catch(error => {
                        console.error('Lỗi khi xóa người dùng:', error);
                        showToast(error.message, 'error');
                    });
                } else {
                    showToast('Đã hủy bỏ thao tác xóa.', 'info');
                }
        });
    }

    function handleToggleStatus(e) {
 const id = e.currentTarget.getAttribute('data-id');
 const isEnabled = e.currentTarget.getAttribute('data-enabled') === 'true';
 const actionText = isEnabled ? 'Khóa' : 'Mở khóa';

 showCustomConfirm(`Bạn có chắc muốn ${actionText} người dùng ID: ${id} không?`, (confirmed) => {
 if (!confirmed) return;

 fetch(`${USER_API_URL}/${id}/toggle-status`, {
 method: 'PUT', // Hoặc PATCH tùy theo API bạn tạo
headers: { 'Authorization': `Bearer ${jwtToken}` }
 })
 .then(response => {
 if (!response.ok) {
 throw new Error(`Lỗi ${response.status}`);
 }
 showToast(`Đã ${actionText} người dùng thành công!`, 'success');
 allUsers = []; // Xóa cache
 fetchUsers(); // Tải lại toàn bộ
 })
 .catch(error => {
 console.error(`Lỗi khi ${actionText} user:`, error);
 showToast(`Không thể ${actionText}: ${error.message}`, 'error');
 });
 });
 }
    document.removeEventListener('click', handleUserClick);
    function handleUserClick(event) {
        if (!document.querySelector('#users-table')) {
            console.log('DEBUG: Bỏ qua handleUserClick vì không ở trang users.');
            return;
        }
        const target = event.target.closest('button');
        if (!target) return;

        if (target.classList.contains('edit-btn')) {
            event.preventDefault();
            console.log('LOG CLICK: Nút Sửa người dùng được bắt.');
            handleEdit({ currentTarget: target });
        }
        // === THÊM KHỐI NÀY ===
  else if (target.classList.contains('toggle-status-btn')) {
 event.preventDefault();
 handleToggleStatus({ currentTarget: target });
 // =====================

 } else if (target.classList.contains('delete-btn')) {
            event.preventDefault();
            console.log('LOG CLICK: Nút Xóa người dùng được bắt.');
            handleDelete({ currentTarget: target });
        }
    }
    document.addEventListener('click', handleUserClick);

// 1. Gán sự kiện cho Filter/Sort (MỚI)
 filterSearchInput.addEventListener('input', () => renderAndFilterUsers(0));
 filterSortSelect.addEventListener('change', () => renderAndFilterUsers(0));
 filterRole.addEventListener('change', () => renderAndFilterUsers(0));

 // 2. Gán sự kiện Phân trang (SỬA LẠI)
 prevPageBtn.addEventListener('click', () => {
        if (currentPage > 0) {
            renderAndFilterUsers(currentPage - 1);
        }
    });
 nextPageBtn.addEventListener('click', () => {
 const totalPages = Math.ceil(filteredUsers.length / pageSize); // Lấy từ mảng đã lọc
        if (currentPage < totalPages - 1) {
            renderAndFilterUsers(currentPage + 1);
        }
    });
    fetchUsers();
}

export { initUserPage };