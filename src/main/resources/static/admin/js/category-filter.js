let originalCategories = [];
let filteredCategories = [];
let currentPage = 0;
const pageSize = 5;
let updateTableCallback = null;

function initCategoryFilter(data, callback) {
    console.log('Filter: Nhận dữ liệu:', data.content.length, 'danh mục');

    originalCategories = [...data.content];
    filteredCategories = [...originalCategories];
    updateTableCallback = callback;

    updateTableAndPagination();

    const searchInput = document.getElementById('category-search');
    const sortSelect = document.getElementById('sort-categories');

    if (!searchInput || !sortSelect) {
        console.warn('Thiếu input tìm kiếm hoặc sắp xếp');
        return;
    }

    searchInput.addEventListener('input', () => {
        const query = removeVietnameseAccents(searchInput.value.trim().toLowerCase());
        filteredCategories = query
            ? originalCategories.filter(cat =>
                removeVietnameseAccents(cat.tenDanhMuc.toLowerCase()).includes(query)
              )
            : [...originalCategories];
        currentPage = 0;
        updateTableAndPagination();
    });

    sortSelect.addEventListener('change', () => {
        const value = sortSelect.value;
        if (value === 'az') {
            filteredCategories.sort((a, b) => a.tenDanhMuc.localeCompare(b.tenDanhMuc));
        } else if (value === 'za') {
            filteredCategories.sort((a, b) => b.tenDanhMuc.localeCompare(a.tenDanhMuc));
        } else {
            filteredCategories = [...originalCategories];
        }
        currentPage = 0;
        updateTableAndPagination();
    });
}

function updateTableAndPagination() {
    if (!updateTableCallback) return;

    const totalItems = filteredCategories.length;
    const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
    const start = currentPage * pageSize;
    const end = Math.min(start + pageSize, totalItems);

    const pageData = {
        content: filteredCategories.slice(start, end),
        number: currentPage,
        totalPages: totalPages,
        totalElements: totalItems,
        first: currentPage === 0,
        last: currentPage === totalPages - 1
    };

    updateTableCallback(pageData.content, pageData);

    // CẬP NHẬT NÚT PHÂN TRANG
    const prevBtn = document.getElementById('prev-page-btn');
    const nextBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');

    if (prevBtn) prevBtn.disabled = currentPage === 0;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages - 1;
    if (pageInfo) pageInfo.textContent = `Trang ${currentPage + 1} / ${totalPages}`;
}

// PUBLIC: Cập nhật sau CRUD
window.updateCategoryFilterData = function(newData) {
    originalCategories = [...newData.content];
    filteredCategories = [...originalCategories];
    document.getElementById('category-search')?.value = '';
    document.getElementById('sort-categories')?.value = '';
    currentPage = 0;
    updateTableAndPagination();
};

function removeVietnameseAccents(str) {
    return str
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd')
        .replace(/Đ/g, 'D');
}

export { initCategoryFilter };