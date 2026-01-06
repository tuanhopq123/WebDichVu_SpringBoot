import { showToast } from './toast.js';
import { fetchWithAuthCheck } from './api.js';
function initServicePage() {
    console.log('DEBUG: Khởi tạo trang Services.');

    const SERVICE_API_URL = '/api/admin/services';
    const CATEGORY_API_URL = '/api/admin/categories/all';
    const jwtToken = localStorage.getItem('jwtToken');

    const servicesTableBody = document.querySelector('#services-table tbody');
    const serviceModal = document.getElementById('service-modal');
    const customConfirmModal = document.getElementById('custom-confirm-modal');
    const descriptionModal = document.getElementById('description-modal');
    const serviceForm = document.getElementById('service-form');
    const modalTitle = document.getElementById('modal-title');
    const addServiceBtn = document.getElementById('add-service-btn');
    const imageFileInput = document.getElementById('imageFile');
    const previewImg = document.getElementById('preview-img');
    const categorySelect = document.getElementById('categoryId');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');
    const searchInput = document.getElementById('service-search');
    const sortSelect = document.getElementById('sort-services');

    let currentPage = 0;
    const pageSize = 4;
    let confirmCallback = null;
    let cachedCategories = null;
    let allServices = []; // TOÀN BỘ DỊCH VỤ
    let filteredServices = [];

    if (!serviceModal || !addServiceBtn || !servicesTableBody || !serviceForm || !categorySelect || !prevPageBtn || !nextPageBtn || !pageInfo || !searchInput || !sortSelect) {
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

    // === RENDER + FILTER + SORT + PAGINATION ===
    function renderFilteredServices(page = 0) {
        let filtered = [...allServices];

        // 1. TÌM KIẾM
        const query = searchInput.value.trim();
        if (query) {
            const cleanQuery = removeAccents(query);
            filtered = filtered.filter(service =>
                removeAccents(service.tenDichVu).includes(cleanQuery)
            );
        }

        // 2. SẮP XẾP
        const sortValue = sortSelect.value;
        if (sortValue === 'az') {
            filtered.sort((a, b) => a.tenDichVu.localeCompare(b.tenDichVu));
        } else if (sortValue === 'za') {
            filtered.sort((a, b) => b.tenDichVu.localeCompare(a.tenDichVu));
        } else if (sortValue === 'price-asc') {
            filtered.sort((a, b) => a.giaCoBan - b.giaCoBan);
        } else if (sortValue === 'price-desc') {
            filtered.sort((a, b) => b.giaCoBan - a.giaCoBan);
        }

        filteredServices = filtered;

        // 3. PHÂN TRANG
        const totalItems = filtered.length;
        const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
        const start = page * pageSize;
        const end = Math.min(start + pageSize, totalItems);
        const pageItems = filtered.slice(start, end);

        // Render bảng
        servicesTableBody.innerHTML = '';
        if (pageItems.length === 0) {
            servicesTableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;">Không tìm thấy dịch vụ nào.</td></tr>`;
        } else {
            const formatCurrency = (amount) => {
                if (!amount) return '—';
                return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
            };

            const formatTime = (time) => {
                if (!time) return '—';
                if (typeof time === 'number') return `${time} phút`;
                if (typeof time === 'string') {
                    const match = time.match(/(\d+)\s*(tiếng|phút)/i);
                    if (match) {
                        const value = parseInt(match[1]);
                        const unit = match[2].toLowerCase();
                        return unit === 'tiếng' ? `${value * 60} phút` : `${value} phút`;
                    }
                    if (!isNaN(parseFloat(time))) return `${parseFloat(time)} phút`;
                }
                return time;
            };

            pageItems.forEach(service => {
                const moTa = service.moTa || 'Không có mô tả.';
                const moTaSafe = moTa.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
                let imageUrl;
                const rawUrl = service.imageURL;
                if (!rawUrl) {
        // Trường hợp 1: Không có ảnh trong DB -> Dùng ảnh mặc định
        imageUrl = 'https://placehold.co/50x50/cccccc/ffffff?text=Service';
    } else if (rawUrl.startsWith('http') || rawUrl.startsWith('https')) {
        // Trường hợp 2: Là link Cloudinary (Online) -> Dùng luôn
        imageUrl = rawUrl;
    } else if (rawUrl.startsWith('/')) {
        // Trường hợp 3: Là đường dẫn tương đối cũ (/assets/...) -> Dùng luôn (Trình duyệt tự hiểu theo domain hiện tại)
        // KHÔNG nối localhost:8080 vào đây nữa
        imageUrl = rawUrl;
    } else {
        // Trường hợp 4: Chỉ là tên file (image.jpg) -> Nối thêm đường dẫn thư mục
        imageUrl = `/assets/images/${rawUrl}`;
    }

                const row = servicesTableBody.insertRow();
                row.innerHTML = `
                    <td>${service.id || 'N/A'}</td>
                    <td style="width: 80px;">
                        <img src="${imageUrl}" 
                             onerror="this.onerror=null;this.src='https://placehold.co/50x50/cccccc/ffffff?text=Service';"
                             alt="${service.tenDichVu || ''}" 
                             style="width: 50px; height: 50px; object-fit: cover; border-radius: 4px;">
                    </td>
                    <td>${service.tenDichVu || '—'}</td>
                    <td>${formatCurrency(service.giaCoBan)}</td>
                    <td>${formatTime(service.thoiGianHoanThanh)}</td>
                    <td>${service.category?.tenDanhMuc || '—'}</td>
                    <td class="text-center">
                        <button class="description-icon" 
                                data-title="${service.tenDichVu || 'Dịch vụ'}"
                                data-description="${moTaSafe}"
                                style="cursor: pointer;">
                            <i class="fas fa-eye"></i>
                        </button>
                    </td>
                    <td>
                        <button class="btn btn-info btn-sm edit-btn" data-id="${service.id}"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger btn-sm delete-btn" data-id="${service.id}"><i class="fas fa-trash"></i></button>
                    </td>
                `;
            });
        }

        currentPage = page;
        pageInfo.textContent = `Trang ${page + 1} / ${totalPages}`;
        prevPageBtn.disabled = page === 0;
        nextPageBtn.disabled = page >= totalPages - 1;
    }

    // === LẤY TOÀN BỘ DỊCH VỤ (CHỈ 1 LẦN) ===
    function fetchAllServices() {
        if (!jwtToken) {
            servicesTableBody.innerHTML = `<tr><td colspan="8" style="color:red; text-align:center;">Vui lòng đăng nhập.</td></tr>`;
            return;
        }

        // 1. THÊM HIỆU ỨNG LOADING TẠI ĐÂY
        servicesTableBody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align:center; padding: 20px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; color: #007bff;"></i>
                    <div style="margin-top: 5px;">Đang tải dữ liệu...</div>
                </td>
            </tr>`;

        if (allServices.length === 0) {
            fetchWithAuthCheck(`${SERVICE_API_URL}?page=0&size=1000`, { // Lấy đủ
                headers: { 'Authorization': `Bearer ${jwtToken}` }
            })
            .then(r => r.json())
            .then(data => {
                allServices = data.content || [];
                renderFilteredServices(0);
            })
            .catch(err => {
                servicesTableBody.innerHTML = `<tr><td colspan="8" style="color:red; text-align:center;">${err}</td></tr>`;
                showToast(err, 'error');
            });
        } else {
            renderFilteredServices(currentPage);
        }
    }

    // === TÌM KIẾM + SẮP XẾP LIVE ===
    searchInput.addEventListener('input', () => {
        currentPage = 0;
        renderFilteredServices(0);
    });

    sortSelect.addEventListener('change', () => {
        currentPage = 0;
        renderFilteredServices(0);
    });

    // === PHÂN TRANG ===
    prevPageBtn.addEventListener('click', () => {
        if (currentPage > 0) renderFilteredServices(currentPage - 1);
    });

    nextPageBtn.addEventListener('click', () => {
        const totalPages = Math.ceil(filteredServices.length / pageSize);
        if (currentPage < totalPages - 1) renderFilteredServices(currentPage + 1);
    });

    // === LOAD DANH MỤC ===
    function loadCategories() {
        if (cachedCategories) {
            categorySelect.innerHTML = '<option value="">Chọn danh mục</option>';
            cachedCategories.forEach(cat => {
                const opt = document.createElement('option');
                opt.value = cat.id;
                opt.textContent = cat.tenDanhMuc;
                categorySelect.appendChild(opt);
            });
            return Promise.resolve();
        }
        return fetchWithAuthCheck(CATEGORY_API_URL, { headers: { 'Authorization': `Bearer ${jwtToken}` } })
            .then(r => r.json())
            .then(data => {
                cachedCategories = Array.isArray(data) ? data : (data.content || []);
                categorySelect.innerHTML = '<option value="">Chọn danh mục</option>';
                cachedCategories.forEach(cat => {
                    const opt = document.createElement('option');
                    opt.value = cat.id;
                    opt.textContent = cat.tenDanhMuc;
                    categorySelect.appendChild(opt);
                });
            })
            .catch(() => showToast('Không thể tải danh mục', 'error'));
    }

    // === MODAL & CRUD ===
    function resetServiceForm() {
        serviceForm.reset();
        document.getElementById('service-id').value = '';
        previewImg.src = ''; previewImg.style.display = 'none';
        modalTitle.textContent = 'Thêm Dịch Vụ Mới';
    }

    function openAddServiceModal() {
        resetServiceForm();
        loadCategories();
        serviceModal.style.display = 'flex';
        serviceModal.style.zIndex = '1001';
    }

    function closeAllModals() {
        [serviceModal, customConfirmModal, descriptionModal].forEach(m => m && (m.style.display = 'none'));
    }

    function handleEdit(e) {
        const id = e.currentTarget.getAttribute('data-id');
        
        // 1. Tìm dịch vụ ngay trong mảng allServices (Không cần gọi Server)
        const service = allServices.find(s => s.id == id);

        if (!service) {
            showToast('Không tìm thấy dữ liệu dịch vụ này.', 'error');
            return;
        }

        // 2. Điền dữ liệu vào Form ngay lập tức
        document.getElementById('service-id').value = service.id;
        document.getElementById('tenDichVu').value = service.tenDichVu || '';
        document.getElementById('giaCoBan').value = service.giaCoBan || '';
        document.getElementById('thoiGianHoanThanh').value = service.thoiGianHoanThanh || '';
        document.getElementById('moTa').value = service.moTa || '';

        // 3. Xử lý ảnh Preview
        if (service.imageURL) {
            let imgUrl = service.imageURL;
            // Nếu là ảnh Cloudinary, ta dùng ảnh gốc hoặc ảnh to hơn chút cho nét (w_300)
            if (imgUrl.includes('res.cloudinary.com')) {
                // Xóa các tham số resize nhỏ (nếu có) để lấy ảnh nét hơn cho preview
                imgUrl = imgUrl.replace(/upload\/.*?\//, 'upload/w_300,c_fill,q_auto/');
            } else if (!imgUrl.startsWith('http') && !imgUrl.startsWith('/')) {
                imgUrl = `/assets/images/${imgUrl}`;
            }
            previewImg.src = imgUrl;
            previewImg.style.display = 'block';
        } else {
            previewImg.src = '';
            previewImg.style.display = 'none';
        }

        // 4. Mở Modal NGAY LẬP TỨC (Không chờ đợi gì cả)
        modalTitle.textContent = `Sửa Dịch Vụ: ${service.tenDichVu}`;
        serviceModal.style.display = 'flex';
        serviceModal.style.zIndex = '1001';

        // 5. Load danh mục và set giá trị (Chạy ngầm, người dùng đã thấy modal rồi)
        loadCategories().then(() => {
            if (service.category) {
                categorySelect.value = service.category.id;
            } else {
                categorySelect.value = "";
            }
        });
    }

    function handleDelete(e) {
        const id = e.currentTarget.getAttribute('data-id');
        showCustomConfirm(`Xóa dịch vụ ID: ${id}?`, confirmed => {
            if (!confirmed) return;
            fetch(`${SERVICE_API_URL}/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${jwtToken}` } })
                .then(r => {
                    if (r.ok) {
                        showToast('Xóa thành công!', 'success');
                        allServices = allServices.filter(s => s.id != id);
                        renderFilteredServices(currentPage);
                    } else throw new Error('Không thể xóa');
                })
                .catch(() => showToast('Lỗi xóa', 'error'));
        });
    }

    function showCustomConfirm(msg, cb) {
        document.getElementById('confirm-message').textContent = msg;
        confirmCallback = cb;
        customConfirmModal.style.display = 'flex';
    }

    // === SỰ KIỆN CLICK ===
    addServiceBtn.addEventListener('click', e => { e.preventDefault(); openAddServiceModal(); });
    document.querySelectorAll('.close-btn').forEach(b => b.addEventListener('click', closeAllModals));
    window.addEventListener('click', e => { if ([serviceModal, customConfirmModal, descriptionModal].includes(e.target)) closeAllModals(); });

    document.removeEventListener('click', handleServiceClick);
    function handleServiceClick(e) {
        if (!document.querySelector('#services-table')) return;
        const btn = e.target.closest('button');
        if (!btn) return;
        if (btn.classList.contains('edit-btn')) handleEdit({ currentTarget: btn });
        if (btn.classList.contains('delete-btn')) handleDelete({ currentTarget: btn });
        if (btn.classList.contains('description-icon')) {
            const title = btn.getAttribute('data-title');
            const desc = btn.getAttribute('data-description');
            document.getElementById('description-title').textContent = `Chi Tiết: ${title}`;
            document.getElementById('description-content').textContent = desc;
            descriptionModal.style.display = 'flex';
            descriptionModal.style.zIndex = '1001';
        }
    }
    document.addEventListener('click', handleServiceClick);

    // === FORM SUBMIT ===
    serviceForm.addEventListener('submit', e => {
        e.preventDefault();
        
        // 1. Hiệu ứng UX: Khóa nút và hiện icon xoay để biết đang xử lý
        const submitBtn = serviceForm.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xử lý...';

        const id = document.getElementById('service-id')?.value;
        const method = id ? 'PUT' : 'POST';
        const url = id ? `${SERVICE_API_URL}/${id}` : SERVICE_API_URL;
        
        // Validate dữ liệu
        const ten = document.getElementById('tenDichVu')?.value.trim();
        const gia = parseInt(document.getElementById('giaCoBan')?.value);
        const thoiGian = document.getElementById('thoiGianHoanThanh')?.value;
        const catId = document.getElementById('categoryId')?.value;
        const moTa = document.getElementById('moTa')?.value || '';

        if (!ten || isNaN(gia) || gia <= 0 || !catId) {
            showToast('Vui lòng điền đầy đủ thông tin hợp lệ.', 'error');
            submitBtn.disabled = false; 
            submitBtn.textContent = originalText;
            return;
        }

        const formData = new FormData();
        formData.append('tenDichVu', ten);
        formData.append('giaCoBan', gia);
        formData.append('thoiGianHoanThanh', thoiGian);
        formData.append('moTa', moTa);
        formData.append('categoryId', catId);
        if (imageFileInput.files[0]) formData.append('imageFile', imageFileInput.files[0]);

        // 2. Gửi Request
        fetch(url, { method, headers: { 'Authorization': `Bearer ${jwtToken}` }, body: formData })
            .then(r => r.ok ? r.json() : r.json().then(err => { throw new Error(err.message) }))
            .then(savedService => {
                // === ĐOẠN NÀY LÀ MẤU CHỐT ĐỂ NHANH ===
                
                if (id) {
                    // TRƯỜNG HỢP SỬA: Tìm dòng cũ trong mảng và cập nhật lại
                    // Giúp cập nhật ngay lập tức mà không cần F5
                    const index = allServices.findIndex(s => s.id == id);
                    if (index !== -1) {
                        allServices[index] = savedService; 
                    }
                } else {
                    // TRƯỜNG HỢP THÊM: Chèn cái mới lên đầu danh sách
                    allServices.unshift(savedService);
                }
                
                // Vẽ lại bảng bằng dữ liệu có sẵn trong RAM (Cực nhanh)
                // Không gọi fetchAllServices() nữa
                renderFilteredServices(currentPage); 
                
                showToast('Lưu thành công!', 'success');
                closeAllModals();
            })
            .catch(err => {
                console.error(err);
                showToast(err.message || 'Có lỗi xảy ra', 'error');
            })
            .finally(() => {
                // Trả lại trạng thái nút ban đầu
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            });
    });

    // === KHỞI TẢO ===
    fetchAllServices();
    console.log('DEBUG: initServicePage hoàn tất.');
}

export { initServicePage };