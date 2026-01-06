import { showToast } from './toast.js';
import { fetchWithAuthCheck } from './api.js';

let currentChatUser = null;

export function initServiceChat() {
    console.log('🚀 Init Admin Chat...');

    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        showToast("Lỗi: Thiếu thư viện Chat. F5 lại trang.", "error");
        return;
    }

    // Dù đang kết nối hay không, cứ gọi disconnect cho chắc ăn
    disconnectAdminChat();

    setTimeout(() => {
        connectAdminSocket();
        loadRecentChats();
        setupChatUIListeners();
    }, 200);
}

export function disconnectAdminChat() {
    if (window.adminStompClient) {
        try {
            if (window.adminStompClient.connected) {
                window.adminStompClient.disconnect(() => {
                    console.log("🛑 Admin Chat: Disconnected");
                });
            }
        } catch (e) {
            console.error("Lỗi ngắt kết nối:", e);
        }
        window.adminStompClient = null;
    }
    currentChatUser = null;
}

// --- KẾT NỐI SOCKET ---
function connectAdminSocket() {
    const token = localStorage.getItem('jwtToken');
    if (!token) return;

    // Đảm bảo không có kết nối nào khác đang chạy
    if (window.adminStompClient) return;

    const socket = new SockJS('/ws');
    window.adminStompClient = Stomp.over(socket);
    window.adminStompClient.debug = null; 

    window.adminStompClient.connect(
        { 'Authorization': 'Bearer ' + token },
        (frame) => {
            console.log('✅ Socket Connected Success (New Session)');
            
            // Subscribe kênh nhận tin nhắn
            window.adminStompClient.subscribe('/topic/admin/messages', (msg) => {
                const body = JSON.parse(msg.body);
                handleIncomingMessage(body);
            });
        },
        (error) => {
            console.error('❌ Socket Error:', error);
            // Chỉ reconnect nếu biến toàn cục vẫn còn (tức là chưa bị disconnect do chuyển trang)
            if (window.adminStompClient) {
                setTimeout(connectAdminSocket, 5000);
            }
        }
    );
}

function handleIncomingMessage(msg) {
    // Nếu đang mở khung chat của ĐÚNG người đó
    if (currentChatUser && currentChatUser.id == msg.senderId) {
        // Hiện tin nhắn và KHÔNG tăng badge
        renderMessageBubble(msg.content, 'incoming', msg.timestamp, true);
        scrollToBottom();
        
        // Báo đã đọc ngay lập tức
        fetchWithAuthCheck(`/api/chat/mark-read/${msg.senderId}`, { method: 'POST' });
        
    } else {
        // Nếu đang chat người khác hoặc chưa mở chat
        new Audio('https://assets.mixkit.co/sfx/preview/mixkit-software-interface-start-2574.mp3').play().catch(()=>{});
        
        let item = document.getElementById(`user-item-${msg.senderId}`);
        
        if (item) {
            // Cập nhật nội dung preview
            item.querySelector('.chat-preview').innerText = truncate(msg.content, 25);
            
            const badge = item.querySelector('.badge');
            if (badge) {
                let currentCount = parseInt(badge.innerText || '0');
                badge.innerText = currentCount + 1;
                badge.style.display = 'inline-block';
            }
            
            item.parentElement.prepend(item);
            
        } else {
            // Nếu là khách mới chưa có trong list -> Tải lại danh sách
            loadRecentChats();
        }
    }
}

function updateSidebarList(msg) {
    loadRecentChats(); 
}

function loadRecentChats() {
    fetchWithAuthCheck('/api/chat/recent-users')
        .then(r => r.json())
        .then(users => {
            const list = document.getElementById('admin-chat-list');
            if (!list) return;
            list.innerHTML = '';

            if (!users || users.length === 0) {
                list.innerHTML = '<div style="padding:20px;text-align:center;color:#999">Hộp thư trống</div>';
                return;
            }

            users.forEach(u => {
                let userId = u.userId;
                if (!userId) return;

                // Lấy số lượng chưa đọc từ API
                let unreadCount = u.unreadCount || 0;
                let badgeStyle = unreadCount > 0 ? 'inline-block' : 'none';

                const div = document.createElement('div');
                div.className = 'chat-item';
                div.id = `user-item-${userId}`; 
                
                div.innerHTML = `
                    <div style="display:flex;justify-content:space-between;align-items:center;">
                        <h4 style="margin:0;font-weight:600">${escapeHtml(u.userName || 'Khách')}</h4>
                        <span class="badge badge-danger" style="display:${badgeStyle};font-size:11px;">
                            ${unreadCount}
                        </span>
                    </div>
                    <span class="service-tag" style="font-size:11px;color:#4e73df;font-weight:600">${u.serviceName || 'Hỗ trợ'}</span>
                    <p class="chat-preview" style="margin:5px 0 0;font-size:13px;color:#888;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">
                        ${truncate(u.lastMessage, 25)}
                    </p>
                `;
                
                div.onclick = () => selectUserChat(userId, u.userName, u.serviceName);
                list.appendChild(div);
            });
        });
}

function selectUserChat(userId, userName, serviceName) {
    if (!userId) return;
    currentChatUser = { id: userId, name: userName };
    
    document.getElementById('active-chat-user').innerText = userName || 'Khách hàng';
    document.getElementById('active-service-info').innerText = serviceName ? `• ${serviceName}` : '';
    
    document.getElementById('admin-input').disabled = false;
    document.getElementById('admin-send-btn').disabled = false;

    document.querySelectorAll('.chat-item').forEach(el => el.classList.remove('active'));
    const item = document.getElementById(`user-item-${userId}`);
    if (item) {
        item.classList.add('active');
        
        const badge = item.querySelector('.badge');
        if (badge) {
            badge.innerText = '0';
            badge.style.display = 'none';
        }
    }

    loadChatHistory(userId);
    fetchWithAuthCheck(`/api/chat/mark-read/${userId}`, { method: 'POST' });
}

function loadChatHistory(userId) {
    const area = document.getElementById('admin-messages-area');
    area.innerHTML = '<div style="text-align:center;padding:20px;color:#ccc">Đang tải...</div>';

    fetchWithAuthCheck(`/api/chat/history/${userId}`)
        .then(r => r.json())
        .then(msgs => {
            area.innerHTML = '';
            if (!msgs || msgs.length === 0) {
                area.innerHTML = '<div style="text-align:center;padding:20px;color:#ccc">Chưa có tin nhắn</div>';
                return;
            }
            msgs.forEach(msg => {
                const type = msg.role === 'ADMIN' ? 'outgoing' : 'incoming';
                renderMessageBubble(msg.content, type, msg.timestamp, msg.read);
            });
            scrollToBottom();
        });
}

function adminSendMessage() {
    const input = document.getElementById('admin-input');
    const text = input.value.trim();
    if (!text || !currentChatUser) return;

    let adminId = 1; 
    try {
        const token = localStorage.getItem('jwtToken');
        adminId = JSON.parse(atob(token.split('.')[1])).userId;
    } catch(e){}

    const payload = {
        senderId: adminId,
        recipientId: currentChatUser.id,
        content: text,
        role: 'ADMIN',
        senderName: 'Admin'
    };

    if (window.adminStompClient && window.adminStompClient.connected) {
        window.adminStompClient.send("/app/chat", {}, JSON.stringify(payload));
        renderMessageBubble(text, 'outgoing', new Date(), false);
        input.value = '';
        scrollToBottom();
    } else {
        showToast("Mất kết nối chat!", "error");
    }
}

function setupChatUIListeners() {
    const input = document.getElementById('admin-input');
    const btn = document.getElementById('admin-send-btn');

    if(input) {
        const newInput = input.cloneNode(true);
        input.parentNode.replaceChild(newInput, input);
        newInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') adminSendMessage();
        });
    }
    if(btn) {
        const newBtn = btn.cloneNode(true);
        btn.parentNode.replaceChild(newBtn, btn);
        newBtn.addEventListener('click', adminSendMessage);
    }
}

function renderMessageBubble(text, type, time, isRead) {
    const area = document.getElementById('admin-messages-area');
    const div = document.createElement('div');
    div.className = `msg ${type}`;
    
    const t = new Date(time || new Date()).toLocaleTimeString('vi-VN', {hour:'2-digit', minute:'2-digit'});
    
    let statusIcon = '';
    if (type === 'outgoing') {
        statusIcon = isRead 
            ? '<i class="fas fa-check-double" style="margin-left:5px;font-size:10px;color:#fff;"></i>' 
            : '<i class="fas fa-check" style="margin-left:5px;font-size:10px;color:rgba(255,255,255,0.7);"></i>';
    }

    div.innerHTML = `
        ${escapeHtml(text)}
        <div style="display:flex;justify-content:flex-end;align-items:center;margin-top:3px;">
            <span class="msg-time" style="font-size:10px;opacity:0.8;color:${type==='outgoing'?'#fff':'#888'}">${t}</span>
            ${statusIcon}
        </div>
    `;
    area.appendChild(div);
}

function scrollToBottom() {
    const area = document.getElementById('admin-messages-area');
    if(area) area.scrollTop = area.scrollHeight;
}

function escapeHtml(text) { return (text || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"); }
function truncate(str, n) { return (str && str.length > n) ? str.substr(0, n-1) + '...' : str || ''; }