let _fetch; // Biến để giữ hàm fetchWithAuthCheck
let _toast; // Biến để giữ hàm showToast

export function initInvitationsPage(fetchWithAuthCheck, showToast) {
    // Nhận các hàm tiện ích từ router chính
    _fetch = fetchWithAuthCheck;
    _toast = showToast;
    
    fetchAndRenderInvitations();
}

async function fetchAndRenderInvitations() {
    const container = document.getElementById('invitations-list-container');
    if (!container) return;
    
    container.innerHTML = '<p>Đang tải các lời mời...</p>';

    try {
        const response = await _fetch('/api/assignments/my-invitations');
        const invitations = await response.json();

        if (invitations.length === 0) {
            container.innerHTML = '<p>Bạn không có lời mời mới nào.</p>';
            return;
        }
        
        container.innerHTML = ''; // Xóa sạch
        invitations.forEach(invite => {
            container.appendChild(createInvitationCard(invite));
        });

        // Gán sự kiện click cho các nút
        addCardEventListeners();

    } catch (error) {
        console.error('Lỗi tải lời mời:', error);
        container.innerHTML = `<p style="color: red;">Lỗi khi tải lời mời: ${error.message}</p>`;
    }
}

function createInvitationCard(invite) {
    const card = document.createElement('div');
    card.className = 'invitation-card';
    card.setAttribute('data-assignment-id', invite.id); // Lưu ID

    // Lấy thông tin từ đối tượng 'order' lồng bên trong
    const order = invite.order; 
    
    // (Kiểm tra backend của bạn để đảm bảo 'order' và 'order.service' được trả về)
    const serviceName = order?.service?.tenDichVu || 'Không rõ dịch vụ';
    const customerName = order?.user?.hoTen || 'Không rõ khách';
    const time = new Date(order?.thoiGianDat).toLocaleString('vi-VN');

    card.innerHTML = `
        <div class="card-header">
            <h3>Đơn hàng #${order.id}</h3>
        </div>
        <div class="card-body">
            <p><strong>Dịch vụ:</strong> ${serviceName}</p>
            <p><strong>Khách hàng:</strong> ${customerName}</p>
            <p><strong>Thời gian hẹn:</strong> ${time}</p>
        </div>
        <div class="card-actions">
            <button class="btn btn-danger btn-reject" data-id="${invite.id}">
                <i class="fas fa-times"></i> Từ chối
            </button>
            <button class="btn btn-success btn-accept" data-id="${invite.id}">
                <i class="fas fa-check"></i> Chấp nhận
            </button>
        </div>
    `;
    return card;
}

function addCardEventListeners() {
    const container = document.getElementById('invitations-list-container');

    container.addEventListener('click', async (e) => {
        const button = e.target;
        
        const assignmentId = button.dataset.id;
        if (!assignmentId) return; // Click vào vùng khác

        // Vô hiệu hóa cả 2 nút
        const cardActions = button.closest('.card-actions');
        cardActions.querySelectorAll('button').forEach(btn => btn.disabled = true);

        if (button.classList.contains('btn-accept')) {
            // Xử lý Chấp nhận
            try {
                // API (Backend) chúng ta đã tạo
                await _fetch(`/api/assignments/${assignmentId}/accept`, { method: 'POST' });
                _toast('Đã chấp nhận việc!', 'success');
                // Xóa thẻ này khỏi giao diện
                button.closest('.invitation-card').remove();
            } catch (error) {
                _toast(`Lỗi: ${error.message}`, 'error');
                cardActions.querySelectorAll('button').forEach(btn => btn.disabled = false); // Mở lại nút
            }

        } else if (button.classList.contains('btn-reject')) {
            // Xử lý Từ chối
             try {
                await _fetch(`/api/assignments/${assignmentId}/reject`, { method: 'POST' });
                _toast('Đã từ chối việc.', 'success');
                button.closest('.invitation-card').remove();
            } catch (error) {
                _toast(`Lỗi: ${error.message}`, 'error');
                cardActions.querySelectorAll('button').forEach(btn => btn.disabled = false); // Mở lại nút
            }
        }
    });
}