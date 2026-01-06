let allServices = [];
let allCategories = [];
let displayedCategoriesCount = 5; // Số danh mục hiển thị ban đầu
let currentPage = 1; // Trang hiện tại cho dịch vụ
const servicesPerPage = 6; // Tối đa 6 dịch vụ/trang

// Hàm loại bỏ dấu tiếng Việt
function removeDiacritics(str) {
    return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D');
}
/**
 * Hiển thị danh sách dịch vụ ra màn hình (hiển thị theo trang)
 * @param {Array} servicesToDisplay - Mảng các dịch vụ đã lọc và sắp xếp
 */
function renderServices(servicesToDisplay) {
    const servicesGrid = document.getElementById('all-services-grid');
    const resultsCount = document.getElementById('results-count');
    const noResultsMessage = document.getElementById('no-results-message');
    const paginationControls = document.getElementById('pagination-controls');
    const currentPageInfo = document.getElementById('current-page-info');
    const prevPageBtn = document.getElementById('prev-page');
    const nextPageBtn = document.getElementById('next-page');

    resultsCount.textContent = servicesToDisplay.length;

    if (servicesToDisplay.length === 0) {
        servicesGrid.innerHTML = '';
        noResultsMessage.style.display = 'block';
        paginationControls.style.display = 'none';
        return;
    }

    noResultsMessage.style.display = 'none';
    paginationControls.style.display = 'block';

    const totalPages = Math.ceil(servicesToDisplay.length / servicesPerPage);
    currentPageInfo.textContent = `Trang ${currentPage} / ${totalPages}`;
    prevPageBtn.disabled = currentPage === 1;
    nextPageBtn.disabled = currentPage === totalPages;

    const startIndex = (currentPage - 1) * servicesPerPage;
    const endIndex = startIndex + servicesPerPage;
    const paginatedServices = servicesToDisplay.slice(startIndex, endIndex);

    servicesGrid.innerHTML = paginatedServices.map(service => `
        <div class="service-card">
            <div class="service-image">
                <img src="${service.imageURL || '/assets/images/service-default.jpg'}" alt="${service.tenDichVu}">
                <div class="service-category">${service.category ? service.category.tenDanhMuc : 'Chưa phân loại'}</div>
            </div>
            <div class="service-content">
                <h3 class="service-title">${truncateText(service.tenDichVu, 4)}</h3>
                <div class="service-meta">
                    <span class="service-price">${new Intl.NumberFormat('vi-VN').format(service.giaCoBan)} đ</span>
                    <span class="service-duration"><i class="far fa-clock"></i> ${service.thoiGianHoanThanh || 'N/A'} phút</span>
                </div>
                <p class="service-description">${truncateText(service.moTa, 5)}</p>
                <div class="service-actions">
                    <a href="/service_detail.html?id=${service.id}" class="btn btn-outline">Chi tiết</a>
                    <a href="/booking.html?service_id=${service.id}" class="btn btn-primary">Đặt lịch</a>
                </div>
            </div>
        </div>
    `).join('');
}

/**
 * Cắt văn bản với số lượng chữ tối đa
 * @param {string} text - Văn bản cần cắt
 * @param {number} maxWords - Số lượng từ tối đa (4 cho tên dịch vụ, 5 cho mô tả)
 * @return {string} - Văn bản đã cắt
 */
function truncateText(text, maxWords) {
    if (!text) return 'Không có mô tả';
    const words = text.trim().split(/\s+/);
    if (words.length > maxWords) {
        return words.slice(0, maxWords).join(' ') + '...';
    }
    return text;
}

/**
 * Hiển thị danh mục theo số lượng giới hạn
 * @param {Array} categories - Mảng các danh mục
 * @param {number} count - Số lượng danh mục hiển thị
 */
function renderCategories(categories, count = 5) {
    const categoryFilters = document.getElementById('category-filters');
    const loadMoreBtn = document.getElementById('load-more-categories');
    let categoryHTML = `
        <label>
            <input type="radio" name="category" value="all" checked> Tất cả
        </label>
    `;
    if (Array.isArray(categories)) {
        const visibleCategories = categories.slice(0, count);
        categoryHTML += visibleCategories.map(cat => `
            <label>
                <input type="radio" name="category" value="${cat.id}"> ${cat.tenDanhMuc}
            </label>
        `).join('');
    }
    categoryFilters.innerHTML = categoryHTML;

    if (count < categories.length) {
        loadMoreBtn.style.display = 'block';
    } else {
        loadMoreBtn.style.display = 'none';
    }
}

function applyFilters() {
    let filteredServices = [...allServices];
    
    const searchQuery = document.getElementById('search-input').value.toLowerCase().trim();
    const selectedCategory = document.querySelector('input[name="category"]:checked').value;
    const priceSort = document.getElementById('price-sort').value;
    const nameSort = document.getElementById('name-sort').value;

    if (searchQuery) {
        const queryWithoutDiacritics = removeDiacritics(searchQuery);
        filteredServices = filteredServices.filter(service => 
            removeDiacritics(service.tenDichVu.toLowerCase()).includes(queryWithoutDiacritics)
        );
    }

    if (selectedCategory !== 'all') {
        filteredServices = filteredServices.filter(service => 
            service.category && service.category.id == selectedCategory
        );
    }

    if (priceSort !== 'default') {
        if (priceSort === 'asc') {
            filteredServices.sort((a, b) => Number(a.giaCoBan) - Number(b.giaCoBan));
        } else {
            filteredServices.sort((a, b) => Number(b.giaCoBan) - Number(a.giaCoBan));
        }
    } else if (nameSort !== 'default') {
        if (nameSort === 'asc') {
            filteredServices.sort((a, b) => a.tenDichVu.localeCompare(b.tenDichVu, 'vi'));
        } else {
            filteredServices.sort((a, b) => b.tenDichVu.localeCompare(a.tenDichVu, 'vi'));
        }
    }
    
    renderServices(filteredServices);
}

function setupEventListeners() {
    const searchInput = document.getElementById('search-input');
    const categoryFilters = document.getElementById('category-filters');
    const priceSort = document.getElementById('price-sort');
    const nameSort = document.getElementById('name-sort');
    const resetBtn = document.getElementById('reset-filters');
    const loadMoreCategories = document.getElementById('load-more-categories');
    const prevPageBtn = document.getElementById('prev-page');
    const nextPageBtn = document.getElementById('next-page');
    
    searchInput.addEventListener('input', applyFilters);
    categoryFilters.addEventListener('change', applyFilters);
    
    priceSort.addEventListener('change', () => {
        nameSort.value = 'default';
        applyFilters();
    });
    
    nameSort.addEventListener('change', () => {
        priceSort.value = 'default';
        applyFilters();
    });

    resetBtn.addEventListener('click', () => {
        searchInput.value = '';
        document.querySelector('input[name="category"][value="all"]').checked = true;
        priceSort.value = 'default';
        nameSort.value = 'default';
        currentPage = 1;
        applyFilters();
    });

    loadMoreCategories.addEventListener('click', () => {
        displayedCategoriesCount += 5;
        renderCategories(allCategories, displayedCategoriesCount);
    });

    prevPageBtn.addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            applyFilters();
        }
    });

    nextPageBtn.addEventListener('click', () => {
        const totalPages = Math.ceil(allServices.length / servicesPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            applyFilters();
        }
    });
}

async function initializeServicesPage() {
    try {
        const [servicesResponse, categoriesResponse] = await Promise.all([
            fetch('/api/services?page=0&size=100'),
            fetch('/api/categories')
        ]);

        if (!servicesResponse.ok || !categoriesResponse.ok) {
            throw new Error('Lỗi mạng khi tải dữ liệu.');
        }
        
        const servicesData = await servicesResponse.json();
        const categoriesData = await categoriesResponse.json();
        
        allServices = Array.isArray(servicesData.content) ? servicesData.content : (Array.isArray(servicesData) ? servicesData : []);
        allCategories = Array.isArray(categoriesData) ? categoriesData : [];

        renderCategories(allCategories, displayedCategoriesCount);
        applyFilters();
        
        setupEventListeners();

    } catch (error) {
        console.error('Lỗi khi khởi tạo trang dịch vụ:', error);
        document.getElementById('all-services-grid').innerHTML = 
            '<p style="text-align: center; color: red;">Đã xảy ra lỗi khi tải dịch vụ. Vui lòng thử lại sau.</p>';
    }
}

function loadAuthSection() {
    const token = localStorage.getItem('jwtToken');
    const authSection = document.getElementById('auth-section');

    if (token) {
        fetch('/api/users/me', { headers: { 'Authorization': `Bearer ${token}` } })
        .then(response => {
            if (!response.ok) throw new Error('Unauthorized');
            return response.json();
        })
        .then(user => {
            const avatar = user.avatar || '/assets/images/default-avatar.png';
            const displayName = user.hoTen || 'Người dùng';
            authSection.innerHTML = `
                <div class="header-actions">
                    <div class="notification-container">
                        <div class="notification-icon"><i class="fas fa-bell"></i><span class="notification-badge" style="display: none;"></span></div>
                        <div class="notification-dropdown">
                            <div class="notification-header"><h3>Thông báo</h3><button class="mark-all-read">Đánh dấu đã đọc</button></div>
                            <div class="notification-list" id="notification-list"></div>
                            <div class="notification-footer"><a href="/notifications.html">Xem tất cả</a></div>
                        </div>
                    </div>
                    <div class="user-menu-container">
                        <div class="user-menu-trigger">
                            <img src="${avatar}" alt="Avatar" class="user-avatar">
                            <span class="user-name">${displayName}</span>
                            <i class="fas fa-chevron-down"></i>
                        </div>
                        <div class="user-dropdown">
                            <a href="/account.html" class="user-dropdown-item"><i class="fas fa-user"></i> Hồ sơ cá nhân</a>
                            <a href="/my_bookings.html" class="user-dropdown-item"><i class="fas fa-calendar-check"></i> Đơn đặt dịch vụ</a>
                            <a href="/change_password.html" class="user-dropdown-item"><i class="fas fa-key"></i> Đổi mật khẩu</a>
                            <div class="dropdown-divider"></div>
                            <a href="#" class="user-dropdown-item logout" onclick="logout(event)"><i class="fas fa-sign-out-alt"></i> Đăng xuất</a>
                        </div>
                    </div>
                </div>`;
        })
        .catch(() => {
            localStorage.removeItem('jwtToken');
            authSection.innerHTML = `<div class="auth-buttons"><a href="/login.html" class="btn btn-outline">Đăng nhập</a><a href="/register.html" class="btn btn-primary">Đăng ký</a></div>`;
        });
    } else {
        authSection.innerHTML = `<div class="auth-buttons"><a href="/login.html" class="btn btn-outline">Đăng nhập</a><a href="/register.html" class="btn btn-primary">Đăng ký</a></div>`;
    }
}

function logout(event) {
    event.preventDefault();
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('userRole');
    window.location.href = '/login.html';
}

function loadFooterServices() {
    fetch('/api/services?page=0&size=5')
    .then(response => response.json())
    .then(services => {
        const footerServices = document.getElementById('footer-services');
        if (footerServices) {
            const data = Array.isArray(services.content) ? services.content : (Array.isArray(services) ? services : []);
            footerServices.innerHTML = data.map(service => `
                <li><a href="/service_detail.html?id=${service.id}">${truncateText(service.tenDichVu, 4)}</a></li>
            `).join('');
        }
    }).catch(error => console.error('Lỗi lấy dịch vụ footer:', error));
}

document.addEventListener('DOMContentLoaded', () => {
    loadAuthSection();
    loadFooterServices();
    initializeServicesPage();
});
