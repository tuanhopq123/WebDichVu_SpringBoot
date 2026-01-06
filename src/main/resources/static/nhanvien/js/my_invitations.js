import { fetchWithAuthCheck } from './auth.js';
import { showToast } from './toast.js';
import { formatTimeAgo, showOrderDetails } from './utils.js';

let allInvitations = [];
let currentPage = 1;
const pageSize = 6;
let currentSearchTerm = "";

function normalizeString(str) {
    if (!str) return '';
    return str
        .toLowerCase()
        .normalize("NFD") // Tách dấu
        .replace(/[\u0300-\u036f]/g, ""); // Xóa dấu
}

export async function loadMyInvitations() {
    console.log("Đang tải trang Lời mời (phiên bản nâng cấp)...");
    const container = document.getElementById('invitations-list-container');
    container.innerHTML = "<p>Đang tải lời mời của bạn...</p>";

    setupSearchListener();

    try {
        const response = await fetchWithAuthCheck('/api/assignments/my-invitations', {
            method: 'GET'
        });
        if (!response.ok) throw new Error('Không thể tải lời mời.');

        allInvitations = await response.json();

        updateBadgeCount(allInvitations.length);

        renderPage(); 

    } catch (error) {
        console.error('Lỗi tải lời mời:', error);
        container.innerHTML = "<p>Lỗi khi tải dữ liệu. Vui lòng thử lại.</p>";
    }
}

function renderPage() {
    // 1. Lọc (Filter)
    const normalizedSearch = normalizeString(currentSearchTerm);
    let filteredInvitations = allInvitations;

    if (normalizedSearch) {
        filteredInvitations = allInvitations.filter(assignment => {
            const order = assignment.order;
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

    // 2. Phân trang (Paginate)
    const totalItems = filteredInvitations.length;
    const totalPages = Math.ceil(totalItems / pageSize);

    // Đảm bảo trang hiện tại hợp lệ (nếu xóa hết item ở trang cuối)
    if (currentPage > totalPages) {
        currentPage = totalPages;
    }
    if (currentPage < 1) {
        currentPage = 1;
    }

    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const paginatedItems = filteredInvitations.slice(startIndex, endIndex);

    // 3. Vẽ (Render)
    renderInvitationList(paginatedItems); // Vẽ 6 card
    renderPagination(totalPages, totalItems); // Vẽ nút bấm
}

function renderInvitationList(invitations) {
    const container = document.getElementById('invitations-list-container');
    const template = document.getElementById('invitation-card-template');
    container.innerHTML = ""; // Xóa các card cũ

    if (invitations.length === 0) {
        if (currentSearchTerm) {
            container.innerHTML = `<p>Không tìm thấy lời mời nào khớp với "${currentSearchTerm}".</p>`;
        } else {
            container.innerHTML = "<p>Bạn không có lời mời mới nào.</p>";
        }
        return;
    }

    invitations.forEach(assignment => {
        const order = assignment.order; 
        const card = template.content.cloneNode(true).children[0];

        card.dataset.assignmentId = assignment.id;
        card.dataset.orderId = order.id;

        card.querySelector('.service-name').textContent = order.service.tenDichVu;
        card.querySelector('.time-ago').textContent = formatTimeAgo(assignment.assignedAt);
        card.querySelector('.thoi-gian-dat').textContent = new Date(order.thoiGianDat).toLocaleString('vi-VN');
        card.querySelector('.dia-chi').textContent = order.diaChiDichVu;
        card.querySelector('.ghi-chu').textContent = order.notes || '(Không có ghi chú)';

        card.querySelector('.btn-accept').addEventListener('click', () => handleRespond(assignment.id, 'accept', card));
        card.querySelector('.btn-reject').addEventListener('click', () => handleRespond(assignment.id, 'reject', card));
        card.querySelector('.btn-view-details').addEventListener('click', () => showOrderDetails(order));

        container.appendChild(card);
    });
}

function renderPagination(totalPages, totalItems) {
    const container = document.getElementById('invitation-pagination');
    if (!container) return;
    container.innerHTML = ""; // Xóa nút cũ

    if (totalItems === 0) return; // Không cần phân trang nếu không có gì

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
    pageInfo.textContent = `Trang ${currentPage} / ${totalPages} (Tổng: ${totalItems})`;
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

function setupSearchListener() {
    const searchInput = document.getElementById('invitation-search-input');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            currentSearchTerm = e.target.value;
            currentPage = 1; // Luôn reset về trang 1 khi tìm kiếm
            renderPage(); // Vẽ lại
        });
    }
}

async function handleRespond(assignmentId, responseType, cardElement) {
    const isAccepting = (responseType === 'accept');
    const button = cardElement.querySelector(isAccepting ? '.btn-accept' : '.btn-reject');
    const url = `/api/assignments/${assignmentId}/${responseType}`; 

    button.disabled = true;
    button.textContent = 'Đang xử lý...';

    try {
        const response = await fetchWithAuthCheck(url, { method: 'POST' });

        if (response.ok) {
            showToast(`Bạn đã ${isAccepting ? 'chấp nhận' : 'từ chối'} lời mời.`, 'success');
            
            // CẬP NHẬT QUAN TRỌNG:
            // 1. Xóa item khỏi danh sách GỐC
            allInvitations = allInvitations.filter(a => a.id !== assignmentId);
            
            // 2. Cập nhật lại badge
            updateBadgeCount(allInvitations.length);
            
            // 3. Vẽ lại trang (tự động tính toán lại phân trang)
            renderPage(); 
            
        } else {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Phản hồi thất bại');
        }

    } catch (error) {
        console.error('Lỗi phản hồi lời mời:', error);
        showToast(error.message, 'error');
        // Không cần bật lại nút vì card đã bị xóa
    }
}

function updateBadgeCount(count) {
    const badge = document.getElementById('invitation-count-badge');
    if (badge) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'inline-block' : 'none';
    }
}
