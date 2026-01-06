var stompClient = null;
var currentServiceContext = null; 
var userToken = localStorage.getItem('jwtToken');

// --- 1. KHỞI TẠO ---
document.addEventListener('DOMContentLoaded', () => {
    initServiceChat();
});

function initServiceChat() {
    const chatIcon = document.getElementById('chatIcon');
    const minimizeBtn = document.getElementById('minimizeChat');
    const closeBtn = document.getElementById('closeChat');
    const input = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendMessage');

    // Xử lý Input & Send Button (Tránh lặp sự kiện)
    if (input) {
        const newInput = input.cloneNode(true);
        input.parentNode.replaceChild(newInput, input);
        newInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend(newInput);
            }
        });
    }

    if (sendBtn) {
        const newSendBtn = sendBtn.cloneNode(true);
        sendBtn.parentNode.replaceChild(newSendBtn, sendBtn);
        newSendBtn.addEventListener('click', () => {
            const currentInput = document.getElementById('messageInput');
            handleSend(currentInput);
        });
    }

    // Events mở đóng chat
    if(chatIcon) chatIcon.onclick = (e) => { e.stopPropagation(); toggleChatBox(!isChatOpen()); };
    if(minimizeBtn) minimizeBtn.onclick = (e) => { e.stopPropagation(); toggleChatBox(false); };
    if(closeBtn) closeBtn.onclick = (e) => { e.stopPropagation(); toggleChatBox(false); resetChatContext(); };

    // Tự động kết nối và tải lịch sử
    connectWebSocket();
    loadMyHistory(); // <--- THÊM HÀM NÀY
}

function isChatOpen() {
    return document.getElementById('chatBox').classList.contains('open');
}

// --- 2. TẢI LỊCH SỬ CHAT CỦA TÔI ---
function loadMyHistory() {
    if (!userToken) return;

    fetch('/api/chat/my-history', {
        headers: { 'Authorization': 'Bearer ' + userToken }
    })
    .then(r => r.json())
    .then(messages => {
        const container = document.getElementById('chatMessages');
        // Giữ lại các thông báo system nếu có
        const systemNotes = container.querySelectorAll('.system-note');
        container.innerHTML = ''; 
        systemNotes.forEach(note => container.appendChild(note));

        if(messages && messages.length > 0) {
            messages.forEach(msg => {
                const type = msg.role === 'USER' ? 'user' : 'bot';
                // Truyền msg.read vào để hiển thị trạng thái
                addMessageToUI(msg.content, type, msg.timestamp, msg.read);
            });
            scrollToBottom();
        }
    })
    .catch(err => console.log("Chưa có lịch sử chat"));
}

// --- 3. XỬ LÝ GỬI TIN ---
function handleSend(inputElement) {
    const text = inputElement.value.trim();
    if (!text) return;

    if (!userToken) {
        alert("Vui lòng đăng nhập để chat!");
        window.location.href = '/login.html';
        return;
    }

    // Hiển thị tin nhắn ngay (mặc định chưa xem)
    addMessageToUI(text, 'user', new Date(), false);
    inputElement.value = '';

    sendViaSocket(text);
}

// --- 4. WEBSOCKET ---
function connectWebSocket() {
    if (!userToken) return;
    if (stompClient && stompClient.connected) return;

    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; 

    stompClient.connect({ 'Authorization': 'Bearer ' + userToken }, 
        function (frame) {
            console.log('✅ Connected');
            updateOnlineStatus(true);

            stompClient.subscribe('/user/queue/messages', function (msg) {
                const body = JSON.parse(msg.body);
                addMessageToUI(body.content, 'bot', body.timestamp);
                playNotificationSound();
                
                // Khi nhận tin từ Admin -> Báo đã đọc
                markAsRead();
            });
        }, 
        function (error) {
            console.error('Socket Error', error);
            updateOnlineStatus(false);
            setTimeout(connectWebSocket, 5000);
        }
    );
}

function sendViaSocket(content) {
    if (!stompClient || !stompClient.connected) {
        connectWebSocket();
        setTimeout(() => sendViaSocket(content), 1000);
        return;
    }

    const payload = {
        senderId: getUserIdFromToken(),
        senderName: getUserNameFromToken(),
        content: content,
        role: 'USER',
        serviceId: currentServiceContext ? currentServiceContext.id : null,
        serviceName: currentServiceContext ? currentServiceContext.name : null
    };

    stompClient.send("/app/chat", {}, JSON.stringify(payload));
}

// --- 5. ĐÁNH DẤU ĐÃ ĐỌC ---
function markAsRead() {
    if (!userToken) return;
    fetch('/api/chat/mark-read-user', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + userToken }
    });
}

// --- 6. HÀM UI ---
window.openChatWithService = function(serviceId, serviceName) {
    if (!userToken) {
        alert("Bạn cần đăng nhập!");
        window.location.href = '/login.html';
        return;
    }
    currentServiceContext = { id: serviceId, name: serviceName };
    document.querySelector('.chat-title h3').innerText = `Tư vấn: ${serviceName.substring(0, 15)}...`;
    
    // Thêm note vào chat
    const container = document.getElementById('chatMessages');
    const note = document.createElement('div');
    note.className = 'system-note';
    note.style = 'text-align:center;font-size:11px;color:#888;margin:10px 0;';
    note.innerHTML = `Quan tâm: <strong>${serviceName}</strong>`;
    container.appendChild(note);

    toggleChatBox(true);
    markAsRead(); // Mở chat ra là coi như đã đọc tin cũ
};

function toggleChatBox(show) {
    const box = document.getElementById('chatBox');
    const icon = document.getElementById('chatIcon');
    const input = document.getElementById('messageInput');
    
    if (show) {
        box.classList.add('open');
        icon.classList.add('active');
        input.focus();
        scrollToBottom();
        markAsRead(); // Mở lên là đọc
    } else {
        box.classList.remove('open');
        icon.classList.remove('active');
    }
}

function resetChatContext() {
    currentServiceContext = null;
    document.querySelector('.chat-title h3').innerText = "Hỗ trợ trực tuyến";
}

function addMessageToUI(html, sender, time, isRead = false) {
    const container = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.className = `message ${sender}`;
    
    const timeStr = new Date(time || new Date()).toLocaleTimeString('vi-VN', {hour:'2-digit',minute:'2-digit'});
    
    // Icon trạng thái (Chỉ hiện cho tin mình gửi 'user')
    let statusIcon = '';
    if (sender === 'user') {
        statusIcon = isRead 
            ? '<i class="fas fa-check-double" style="font-size:10px;margin-left:5px;color:#888;"></i>' // Đã xem
            : '<i class="fas fa-check" style="font-size:10px;margin-left:5px;color:#888;"></i>'; // Đã gửi
    }

    div.innerHTML = `
        <div class="message-content">${html}</div>
        <div style="display:flex;align-items:center;justify-content:flex-end;">
            <span class="message-time" style="color:${sender==='user'?'#888':'#888'}">${timeStr}</span>
            ${statusIcon}
        </div>
    `;
    container.appendChild(div);
    scrollToBottom();
}

function scrollToBottom() {
    const container = document.getElementById('chatMessages');
    if(container) container.scrollTop = container.scrollHeight;
}

function updateOnlineStatus(isOnline) {
    const el = document.querySelector('.online-status');
    if(el) el.innerHTML = isOnline 
        ? '<i class="fas fa-circle" style="color:#2ecc71"></i> Đang trực tuyến' 
        : '<i class="fas fa-circle" style="color:#e74a3b"></i> Mất kết nối';
}

function playNotificationSound() {
    new Audio('https://assets.mixkit.co/sfx/preview/mixkit-software-interface-start-2574.mp3').play().catch(()=>{});
}

function getUserIdFromToken() {
    try { return JSON.parse(atob(userToken.split('.')[1])).userId; } catch(e){ return null; }
}
function getUserNameFromToken() {
    try { return JSON.parse(atob(userToken.split('.')[1])).fullName; } catch(e){ return "Khách"; }
}