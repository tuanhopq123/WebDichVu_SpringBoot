import { showToast } from './toast.js';
import { fetchWithAuthCheck } from './api.js';

function initCategoryPage() {
    console.log('DEBUG: Khởi tạo trang Categories.');

    // DÙNG /all → LẤY TOÀN BỘ
    const CATEGORY_API_URL = '/api/admin/categories/all';
    const BASE_CATEGORY_URL = '/api/admin/categories'; // Dùng cho CRUD
    const jwtToken = localStorage.getItem('jwtToken');

    const categoriesTableBody = document.querySelector('#categories-table tbody');
    const categoryModal = document.getElementById('category-modal');
    const customConfirmModal = document.getElementById('custom-confirm-modal');
    const detailModal = document.getElementById('detail-modal');
    const categoryForm = document.getElementById('category-form');
    const modalTitle = document.getElementById('modal-title');
    const addCategoryBtn = document.getElementById('add-category-btn');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');
    const searchInput = document.getElementById('category-search');
    const sortSelect = document.getElementById('sort-categories');

    let currentPage = 0;
    const pageSize = 5;
    let confirmCallback = null;

    let allCategories = [];
    let filteredCategories = [];

    if (!categoryModal || !addCategoryBtn || !categoriesTableBody || !categoryForm || !prevPageBtn || !nextPageBtn || !pageInfo || !detailModal || !searchInput || !sortSelect) {
        console.error('LỖI CRITICAL: Thiếu phần tử DOM cần thiết.');
        return;
    }

    // === XÓA DẤU TIẾNG VIỆT ===
    function removeAccents(str) {
        return str
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/đ/g, 'd')
            .replace(/Đ/g, 'D')
            .toLowerCase();
    }

    // === RENDER + FILTER + SORT + PAGINATION (CLIENT-SIDE) ===
    function renderFilteredCategories(page = 0) {
        let filtered = [...allCategories];

        // 1. TÌM KIẾM (live, không dấu, có số)
        const query = searchInput.value.trim();
        if (query) {
            const cleanQuery = removeAccents(query);
            filtered = filtered.filter(cat =>
                removeAccents(cat.tenDanhMuc).includes(cleanQuery)
            );
        }

        // 2. SẮP XẾP
        const sortValue = sortSelect.value;
        if (sortValue === 'az') {
            filtered.sort((a, b) => a.tenDanhMuc.localeCompare(b.tenDanhMuc));
        } else if (sortValue === 'za') {
            filtered.sort((a, b) => b.tenDanhMuc.localeCompare(a.tenDanhMuc));
        }

        // 3. LƯU LẠI ĐỂ DÙNG CHO PHÂN TRANG
        filteredCategories = filtered;

        // 4. PHÂN TRANG
        const totalItems = filtered.length;
        const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
        const start = page * pageSize;
        const end = Math.min(start + pageSize, totalItems);
        const pageItems = filtered.slice(start, end);

        // Render bảng
        categoriesTableBody.innerHTML = '';
        if (pageItems.length === 0) {
            categoriesTableBody.innerHTML = `<tr><td colspan="4" style="text-align:center;">Không tìm thấy danh mục nào.</td></tr>`;
        } else {
            pageItems.forEach(category => {
                const row = categoriesTableBody.insertRow();
                row.innerHTML = `
                    <td>${category.id || 'N/A'}</td>
                    <td>${category.tenDanhMuc || '—'}</td>
                    <td><button class="btn btn-info btn-sm view-btn" data-id="${category.id}"><i class="fas fa-eye"></i></button></td>
                    <td>
                        <button class="btn btn-info btn-sm edit-btn" data-id="${category.id}"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger btn-sm delete-btn" data-id="${category.id}"><i class="fas fa-trash"></i></button>
                    </td>
                `;
            });
        }

        // Cập nhật phân trang
        currentPage = page;
        pageInfo.textContent = `Trang ${page + 1} / ${totalPages}`;
        prevPageBtn.disabled = page === 0;
        nextPageBtn.disabled = page >= totalPages - 1;
        console.log('DEBUG: Render trang', page + 1, '/', totalPages);
    }

    // === LẤY TOÀN BỘ DANH MỤC (CHỈ 1 LẦN) ===
    function fetchCategories() {
        if (!jwtToken) {
            categoriesTableBody.innerHTML = `<tr><td colspan="4" style="color:red; text-align:center;">Vui lòng đăng nhập.</td></tr>`;
            return;
        }
    // 1. THÊM HIỆU ỨNG LOADING TẠI ĐÂY
        categoriesTableBody.innerHTML = `
            <tr>
                <td colspan="4" style="text-align:center; padding: 20px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #007bff;"></i>
                    <div style="margin-top: 5px;">Đang tải dữ liệu...</div>
                </td>
            </tr>`;
            
        if (allCategories.length === 0) {
            fetchWithAuthCheck(CATEGORY_API_URL, {
                headers: { 'Authorization': `Bearer ${jwtToken}` }
            })
            .then(r => r.json())
            
            .then(data => {
                allCategories = data; // List<Category>
                renderFilteredCategories(0);
            })
            .catch(err => {
                categoriesTableBody.innerHTML = `<tr><td colspan="4" style="color:red; text-align:center;">${err.message}</td></tr>`;
                showToast(err.message, 'error');
            });
        } else {
            renderFilteredCategories(currentPage);
        }
    }

    searchInput.addEventListener('input', () => {
        currentPage = 0;
        renderFilteredCategories(0);
    });

    sortSelect.addEventListener('change', () => {
        currentPage = 0;
        renderFilteredCategories(0);
    });

    prevPageBtn.addEventListener('click', () => {
        if (currentPage > 0) {
            renderFilteredCategories(currentPage - 1);
        }
    });

    nextPageBtn.addEventListener('click', () => {
        const totalPages = Math.ceil(filteredCategories.length / pageSize);
        if (currentPage < totalPages - 1) {
            renderFilteredCategories(currentPage + 1);
        }
    });

    // === MODAL & CRUD ===
    function closeAllModals() {
        [categoryModal, customConfirmModal, detailModal].forEach(m => m && (m.style.display = 'none'));
    }

    function openAddCategoryModal() {
        categoryForm.reset();
        document.getElementById('category-id').value = '';
        modalTitle.textContent = 'Thêm Danh Mục Mới';
        categoryModal.style.display = 'flex';
        categoryModal.style.zIndex = '1001';
    }

    function handleViewDescription(e) {
        const id = e.currentTarget.getAttribute('data-id');
        fetchWithAuthCheck(`${BASE_CATEGORY_URL}/${id}`, { headers: { 'Authorization': `Bearer ${jwtToken}` } })
            .then(r => r.json())
            .then(cat => {
                document.getElementById('detail-description').textContent = cat.moTa || 'Không có mô tả.';
                detailModal.style.display = 'flex';
                detailModal.style.zIndex = '1001';
            })
            .catch(() => showToast('Lỗi tải mô tả', 'error'));
    }

    function handleEdit(e) {
        const id = e.currentTarget.getAttribute('data-id');
        fetchWithAuthCheck(`${BASE_CATEGORY_URL}/${id}`, { headers: { 'Authorization': `Bearer ${jwtToken}` } })
            .then(r =>r.json())
            .then(cat => {
                document.getElementById('category-id').value = cat.id;
                document.getElementById('tenDanhMuc').value = cat.tenDanhMuc || '';
                document.getElementById('moTa').value = cat.moTa || '';
                modalTitle.textContent = `Sửa Danh Mục: ${cat.tenDanhMuc}`;
                categoryModal.style.display = 'flex';
                categoryModal.style.zIndex = '1001';
            })
            .catch(() => showToast('Lỗi tải dữ liệu', 'error'));
    }

    function handleDelete(e) {
        const id = e.currentTarget.getAttribute('data-id');
        showCustomConfirm(`Xóa danh mục ID: ${id}?`, confirmed => {
            if (!confirmed) return;
            fetch(`${BASE_CATEGORY_URL}/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${jwtToken}` }
            })
            .then(r => {
                if (r.ok) {
                    showToast('Xóa thành công!', 'success');
                    allCategories = allCategories.filter(c => c.id != id);
                    renderFilteredCategories(currentPage);
                } else throw new Error('Không thể xóa: còn dịch vụ');
            })
            .catch(() => showToast('Lỗi xóa', 'error'));
        });
    }

    function showCustomConfirm(message, callback) {
        document.getElementById('confirm-message').textContent = message;
        confirmCallback = callback;
        customConfirmModal.style.display = 'flex';
    }

    // === SỰ KIỆN CLICK ===
    addCategoryBtn.addEventListener('click', e => { e.preventDefault(); openAddCategoryModal(); });
    document.querySelectorAll('.close-btn').forEach(b => b.addEventListener('click', closeAllModals));
    window.addEventListener('click', e => { if ([categoryModal, customConfirmModal, detailModal].includes(e.target)) closeAllModals(); });

    document.removeEventListener('click', handleCategoryClick);
    function handleCategoryClick(e) {
        if (!document.querySelector('#categories-table')) {
            return;
        }
        const btn = e.target.closest('button');
        if (!btn) return;
        if (btn.classList.contains('view-btn')) handleViewDescription({ currentTarget: btn });
        if (btn.classList.contains('edit-btn')) handleEdit({ currentTarget: btn });
        if (btn.classList.contains('delete-btn')) handleDelete({ currentTarget: btn });
    }
    document.addEventListener('click', handleCategoryClick);

    // === FORM SUBMIT ===
    categoryForm.addEventListener('submit', e => {
        e.preventDefault();
        const id = document.getElementById('category-id')?.value;
        const method = id ? 'PUT' : 'POST';
        const url = id ? `${BASE_CATEGORY_URL}/${id}` : BASE_CATEGORY_URL;
        const ten = document.getElementById('tenDanhMuc')?.value.trim();
        if (!ten) return showToast('Tên không được trống.', 'error');

        fetch(url, {
            method,
            headers: { 'Authorization': `Bearer ${jwtToken}`, 'Content-Type': 'application/json' },
            body: JSON.stringify({ tenDanhMuc: ten, moTa: document.getElementById('moTa')?.value })
        })
        .then(r => r.ok ? r.json() : r.json().then(err => { throw new Error(err.message) }))
        .then(() => {
            showToast('Lưu thành công!', 'success');
            closeAllModals();
            allCategories = [];
            fetchCategories();
        })
        .catch(err => showToast(err.message, 'error'));
    });

    // === XÁC NHẬN ===
    document.getElementById('confirm-yes-btn')?.addEventListener('click', () => {
        customConfirmModal.style.display = 'none';
        if (confirmCallback) confirmCallback(true);
    });
    document.getElementById('confirm-no-btn')?.addEventListener('click', () => {
        customConfirmModal.style.display = 'none';
        if (confirmCallback) confirmCallback(false);
    });

    // === KHỞI TẢO ===
    fetchCategories();
    console.log('DEBUG: initCategoryPage hoàn tất.');
}

export { initCategoryPage };