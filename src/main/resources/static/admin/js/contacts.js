import { showToast } from './toast.js';

let currentPage = 1;
let currentFilter = 'all';
let currentSearch = '';
let selectedContact = null;

export function initContactsPage() {
    loadContacts();
    setupEventListeners();
}

function setupEventListeners() {
    const searchInput = document.getElementById('contact-search');
    const filterSelect = document.getElementById('contact-filter');

    searchInput?.addEventListener('input', debounce(() => {
        currentSearch = searchInput.value.trim();
        currentPage = 1;
        loadContacts();
    }, 400));

    filterSelect?.addEventListener('change', () => {
        currentFilter = filterSelect.value;
        currentPage = 1;
        loadContacts();
    });

    document.getElementById('prev-page')?.addEventListener('click', () => {
        if (currentPage > 1) { currentPage--; loadContacts(); }
    });

    document.getElementById('next-page')?.addEventListener('click', () => {
        currentPage++;
        loadContacts();
    });

    document.getElementById('refresh-contacts-btn')?.addEventListener('click', () => {
        currentPage = 1;
        currentSearch = '';
        document.getElementById('contact-search').value = '';
        loadContacts();
        showToast('Đã làm mới danh sách', 'success');
    });
}

function loadContacts() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        showToast('Phiên đăng nhập hết hạn!', 'error');
        setTimeout(() => window.location.href = '/login.html', 1500);
        return;
    }

    const url = `/api/admin/contacts?page=${currentPage-1}&size=10&filter=${currentFilter}&search=${encodeURIComponent(currentSearch)}`;

    fetch(url, {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'application/json'
        }
    })
    .then(r => {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
    })
    .then(data => {
        renderTable(data.content || []);
        renderPagination(data.totalPages || 1, (data.pageable?.pageNumber || 0) + 1);
        updateUnreadBadge(data.unreadCount || 0);
    })
    .catch(err => {
        console.error('Load contacts error:', err);
        showToast('Không thể tải dữ liệu', 'error');
    });
}

function renderPagination(totalPages, pageNumber) {
    // Cập nhật text hiển thị số trang (nếu có element hiển thị text)
    const pageInfo = document.getElementById('page-info');
    if (pageInfo) {
        pageInfo.innerText = `Trang ${pageNumber} / ${totalPages}`;
    }

    // Cập nhật trạng thái disable cho nút Previous
    const prevBtn = document.getElementById('prev-page');
    if (prevBtn) {
        // Disable nếu đang ở trang 1
        prevBtn.disabled = pageNumber <= 1;
        // Thêm class style nếu cần (ví dụ cho Bootstrap)
        pageNumber <= 1 ? prevBtn.classList.add('disabled') : prevBtn.classList.remove('disabled');
    }

    // Cập nhật trạng thái disable cho nút Next
    const nextBtn = document.getElementById('next-page');
    if (nextBtn) {
        // Disable nếu đang ở trang cuối
        nextBtn.disabled = pageNumber >= totalPages;
        pageNumber >= totalPages ? nextBtn.classList.add('disabled') : nextBtn.classList.remove('disabled');
    }
}

function updateUnreadBadge(count) {
    // ID trong file HTML của bạn là "unread-contact-count"
    const badge = document.getElementById('unread-contact-count'); 
    
    if (badge) {
        badge.innerText = count;
        // Nếu count > 0 thì hiện, ngược lại thì ẩn (hoặc để số 0 tùy ý bạn)
        if (count > 0) {
            badge.style.display = 'inline-block';
            badge.innerText = count > 99 ? '99+' : count; // Nếu quá nhiều thì hiện 99+ cho đẹp
        } else {
            badge.style.display = 'none';
        }
    }
}

function renderTable(contacts) {
    const tbody = document.querySelector('#contacts-table tbody');
    tbody.innerHTML = '';

    if (!contacts || contacts.length === 0) {
        // Giảm colspan xuống còn 7 vì đã bỏ 1 cột
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:40px;color:#888;">Không có dữ liệu</td></tr>`;
        return;
    }

    contacts.forEach((c, i) => {
        const tr = document.createElement('tr');
        
        // Logic in đậm nếu chưa đọc và chưa trả lời
        if (!c.isRead && !c.adminReply) tr.classList.add('unread');

        // Logic Badge trạng thái
        let statusBadge = '';
        if (c.adminReply) {
            statusBadge = `<span class="badge badge-info" style="background:#36b9cc;color:white;">Đã trả lời</span>`;
        } else if (!c.isRead) {
            statusBadge = `<span class="badge badge-warning">Chưa đọc</span>`;
        } else {
            statusBadge = `<span class="badge badge-success">Đã xem</span>`;
        }

        tr.innerHTML = `
            <td>${i + 1 + (currentPage - 1) * 10}</td>
            <td style="font-size:0.9rem">${new Date(c.createdAt).toLocaleString('vi-VN')}</td>
            <td><strong>${escapeHtml(c.name)}</strong></td>
            <td>${escapeHtml(c.email)}</td>
            <td style="max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                ${c.subject ? escapeHtml(c.subject) : '<i>Không có tiêu đề</i>'}
            </td>
            
            <td style="text-align:center;">
                ${statusBadge}
            </td>
            <td>
                <button class="btn btn-info btn-sm" onclick='openContactModal(${JSON.stringify(c)})'>
                    Xem
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// SỬA MODAL – DÙNG classList
window.openContactModal = function(contact) {
    selectedContact = contact;

    const info = document.getElementById('contact-info');
    info.innerHTML = `
        <div style="background:#f8f9fc;padding:20px;border-radius:10px;margin-bottom:20px;line-height:1.8;">
            <p><strong>Khách:</strong> ${escapeHtml(contact.name)}</p>
            <p><strong>Email:</strong> ${escapeHtml(contact.email)}</p>
            <p><strong>Thời gian:</strong> ${new Date(contact.createdAt).toLocaleString('vi-VN')}</p>
            <p><strong>Tiêu đề:</strong> ${contact.subject ? escapeHtml(contact.subject) : '<i>Không có</i>'}</p>
        </div>
        <hr>
        <p style="font-weight:600;">Nội dung tin nhắn:</p>
        <div style="background:#f8f9fc;padding:18px;border-radius:10px;border-left:5px solid #4e73df;white-space:pre-wrap;">
            ${contact.message.replace(/\n/g, '<br>')}
        </div>
        ${contact.adminReply ? `
        <hr style="margin:25px 0;">
        <p style="font-weight:600;color:#1cc88a;">Đã trả lời lúc: ${new Date(contact.repliedAt).toLocaleString('vi-VN')}</p>
        <div style="background:#e8f5e8;padding:18px;border-radius:10px;border-left:5px solid #1cc88a;white-space:pre-wrap;">
            ${contact.adminReply.replace(/\n/g, '<br>')}
        </div>` : ''}
    `;

    document.getElementById('reply-text').value = '';
    const modal = document.getElementById('contact-modal');
    modal.style.display = 'flex'; // Dùng flex để căn giữa
    modal.classList.add('show');
};

window.closeContactModal = function() {
    const modal = document.getElementById('contact-modal');
    modal.classList.remove('show');
    setTimeout(() => modal.style.display = 'none', 300);
};

window.sendReply = function() {
    const reply = document.getElementById('reply-text').value.trim();
    if (!reply) {
        showToast('Vui lòng nhập nội dung trả lời', 'error');
        return;
    }

    const token = localStorage.getItem('jwtToken');

    fetch(`/api/admin/contacts/${selectedContact.id}/reply`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ reply })
    })
    .then(r => r.json())
    .then(data => {
        if (data.success) {
            showToast('Gửi phản hồi thành công!', 'success');
            closeContactModal();
            loadContacts(); // TẢI LẠI → CẬP NHẬT TRẠNG THÁI
        } else {
            showToast(data.error || 'Gửi thất bại', 'error');
        }
    })
    .catch(() => showToast('Lỗi mạng', 'error'));
};

function escapeHtml(text) {
    if (typeof text !== 'string') return '';
    return text.replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[m]);
}

function debounce(func, wait) {
    let timeout;
    return function(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}