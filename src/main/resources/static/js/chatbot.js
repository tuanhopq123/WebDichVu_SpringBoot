function initChatbot() {
    const chatIcon = document.getElementById('chatIcon');
    const chatBox = document.getElementById('chatBox');
    const minimizeBtn = document.getElementById('minimizeChat');
    const closeBtn = document.getElementById('closeChat');
    const messagesContainer = document.getElementById('chatMessages');
    const input = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendMessage');

    let isOpen = false;

    // MỞ CHAT
    chatIcon.addEventListener('click', (e) => {
        e.stopPropagation();
        isOpen = !isOpen;
        chatBox.classList.toggle('open', isOpen);
        chatIcon.classList.toggle('active', isOpen);
        if (isOpen) {
            input.focus();
            showWelcomeMessage();
            showQuickReplies();
        }
    });

    // THU NHỎ & ĐÓNG
    minimizeBtn.addEventListener('click', (e) => { e.stopPropagation(); isOpen = false; chatBox.classList.remove('open'); chatIcon.classList.remove('active'); });
    closeBtn.addEventListener('click', (e) => { e.stopPropagation(); isOpen = false; chatBox.classList.remove('open'); chatIcon.classList.remove('active'); });

    // GỬI TIN NHẮN – DÙNG API
    function sendMessage() {
        const rawText = input.value;
        const text = rawText.trim();

        if (!rawText || text === '') return;

        addMessage(rawText, 'user');
        input.value = '';
        showTyping();

        fetch('/api/chatbot', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: text })
        })
        .then(r => {
            if (!r.ok) throw new Error('API lỗi: ' + r.status);
            return r.json();
        })
        .then(data => {
            hideTyping();
            addMessage(data.reply || 'Không có phản hồi', 'bot');
        })
        .catch(err => {
            console.error('Lỗi API:', err);
            hideTyping();
            addMessage('Xin lỗi, tôi đang gặp sự cố. Vui lòng gọi <strong>0783 998 046</strong>', 'bot');
        });
    }

    sendBtn.onclick = sendMessage;
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // HIỆN TIN CHÀO
    function showWelcomeMessage() {
        if (messagesContainer.children.length === 0) {
            setTimeout(() => {
                addMessage('Xin chào! Tôi là trợ lý ảo của <strong>Dịch vụ Tại Nhà</strong>. Bạn cần hỗ trợ gì?', 'bot');
            }, 500);
        }
    }
    // QUICK REPLIES
    function showQuickReplies() {
        if (document.querySelector('.quick-replies')) return;
        const container = document.createElement('div');
        container.className = 'quick-replies';
        ['Xem bảng giá', 'Đặt lịch dịch vụ', 'Liên hệ hỗ trợ'].forEach(text => {
            const btn = document.createElement('button');
            btn.className = 'quick-reply-btn';
            btn.textContent = text;
            btn.onclick = () => {
                container.remove();
                if (text.includes('giá')) {
                    addMessage('Bảng giá: <a href="/service_price_list.html" style="color:#007bff; font-weight:500;">Xem ngay</a>', 'bot');
                } else if (text.includes('lịch')) {
                    addMessage('Đặt lịch: <a href="/services.html" style="color:#007bff; font-weight:500;">Chọn dịch vụ</a>', 'bot');
                } else {
                    addMessage('Hotline: <strong>0783 998 046</strong><br>Email: tuanhopq2019@gmail.com', 'bot');
                }
            };
            container.appendChild(btn);
        });
        messagesContainer.appendChild(container);
        scrollToBottom();
    }

    // HIỆN "ĐANG GÕ"
    function showTyping() {
        const typing = document.createElement('div');
        typing.className = 'message bot typing-indicator';
        typing.innerHTML = '<span></span><span></span><span></span>';
        typing.id = 'typing';
        messagesContainer.appendChild(typing);
        scrollToBottom();
    }

    function hideTyping() {
        document.getElementById('typing')?.remove();
    }

    // THÊM TIN NHẮN
    function addMessage(html, sender) {
        const div = document.createElement('div');
        div.className = `message ${sender}`;
        div.innerHTML = `
            <div class="message-content">${html}</div>
            <span class="message-time">${new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
        `;
        messagesContainer.appendChild(div);
        scrollToBottom();
    }

    function scrollToBottom() {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // GỌI KHI MỞ
    showWelcomeMessage();
    showQuickReplies();
}

// THÊM VÀO CUỐI FILE (trước // KHỞI ĐỘNG)
function showQuickOptions(options) {
    // Xóa nút cũ
    document.querySelectorAll('.quick-options').forEach(el => el.remove());

    const container = document.createElement('div');
    container.className = 'quick-replies quick-options';
    options.forEach(opt => {
        const btn = document.createElement('button');
        btn.className = 'quick-reply-btn';
        btn.textContent = opt;
        btn.onclick = () => {
            container.remove();
            input.value = opt;
            sendMessage();
        };
        container.appendChild(btn);
    });
    messagesContainer.appendChild(container);
    scrollToBottom();
}

// SỬA HÀM sendMessage() – THÊM XỬ LÝ OPTIONS
function sendMessage() {
    const rawText = input.value;
    const text = rawText.trim();

    if (!rawText || text === '') return;

    addMessage(rawText, 'user');
    input.value = '';
    showTyping();

    fetch('/api/chatbot', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: text })
    })
    .then(r => r.ok ? r.json() : Promise.reject())
    .then(data => {
        hideTyping();
        addMessage(data.reply || 'Không có phản hồi', 'bot');

        if (data.options && Array.isArray(data.options)) {
            showQuickOptions(data.options);
        }
    })
    .catch(() => {
        hideTyping();
        addMessage('Xin lỗi, tôi đang gặp sự cố. Vui lòng gọi <strong>0783 998 046</strong>', 'bot');
    });
}

document.addEventListener('DOMContentLoaded', () => {
    console.log('%cCHATBOT ĐÃ KHỞI ĐỘNG!', 'color: green; font-size: 16px; font-weight: bold;');
    initChatbot();
});