const API_BASE_URL = '/api';
let cartData = null;
let recommendedServices = [];

const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
};

const truncateText = (text, maxWords) => {
    if (!text) return 'Không có mô tả';
    const words = text.trim().split(/\s+/);
    return words.length > maxWords ? words.slice(0, maxWords).join(' ') + '...' : text;
};

const getOptimizedImage = (url) => {
    if (!url || !url.includes('res.cloudinary.com')) {
        return url || '/assets/images/service-default.jpg';
    }
    return url.replace('/upload/', '/upload/w_400,h_300,c_fill,q_auto,f_auto/');
};

const showLoading = () => {
    document.getElementById('loadingOverlay').style.display = 'flex';
};

const hideLoading = () => {
    document.getElementById('loadingOverlay').style.display = 'none';
};

const showAlert = (message, type = 'success') => {
    const alert = document.getElementById('success-alert');
    const messageEl = document.getElementById('success-message');
    messageEl.textContent = message;
    alert.style.display = 'flex';
    
    setTimeout(() => {
        alert.style.display = 'none';
    }, 3000);
};

const getAuthHeaders = () => {
    const token = localStorage.getItem('jwtToken');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
};

const checkAuth = () => {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        window.location.href = '/login.html';
        return false;
    }
    return true;
};

// Load Cart Data
const loadCart = async () => {
    if (!checkAuth()) return;
    
    showLoading();
    try {
        const response = await fetch(`${API_BASE_URL}/cart`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) throw new Error('Failed to load cart');
        
        cartData = await response.json();
        renderCart();
        updateHeaderCartCount();
    } catch (error) {
        console.error('Error loading cart:', error);
        showAlert('Không thể tải giỏ hàng. Vui lòng thử lại.', 'error');
    } finally {
        hideLoading();
    }
};

// Update Cart Item Quantity
const updateQuantity = async (cartItemId, newQuantity) => {
    if (newQuantity < 1) return;
    
    showLoading();
    try {
        const response = await fetch(`${API_BASE_URL}/cart/items/${cartItemId}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify({ quantity: newQuantity })
        });
        
        if (!response.ok) throw new Error('Failed to update quantity');
        
        await loadCart();
        showAlert('Đã cập nhật số lượng');
    } catch (error) {
        console.error('Error updating quantity:', error);
        showAlert('Không thể cập nhật số lượng', 'error');
    } finally {
        hideLoading();
    }
};

// Remove Cart Item
const removeItem = async (cartItemId) => {
    if (!confirm('Bạn có chắc muốn xóa dịch vụ này khỏi giỏ hàng?')) return;
    
    showLoading();
    try {
        const response = await fetch(`${API_BASE_URL}/cart/items/${cartItemId}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) throw new Error('Failed to remove item');
        
        await loadCart();
        showAlert('Đã xóa dịch vụ khỏi giỏ hàng');
    } catch (error) {
        console.error('Error removing item:', error);
        showAlert('Không thể xóa dịch vụ', 'error');
    } finally {
        hideLoading();
    }
};

// Clear All Cart Items
const clearAllItems = async () => {
    if (!confirm('Bạn có chắc muốn xóa tất cả dịch vụ khỏi giỏ hàng?')) return;
    
    showLoading();
    try {
        const response = await fetch(`${API_BASE_URL}/cart`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) throw new Error('Failed to clear cart');
        
        await loadCart();
        showAlert('Đã xóa tất cả dịch vụ khỏi giỏ hàng');
    } catch (error) {
        console.error('Error clearing cart:', error);
        showAlert('Không thể xóa giỏ hàng', 'error');
    } finally {
        hideLoading();
    }
};

// Load Recommended Services
const loadRecommendedServices = async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/services?page=0&size=4`);
        if (!response.ok) throw new Error('Failed to load services');
        
        const data = await response.json();
        recommendedServices = data.content || [];
        renderRecommendedServices();
    } catch (error) {
        console.error('Error loading recommended services:', error);
    }
};

const renderCart = () => {
    const emptyCart = document.getElementById('emptyCart');
    const cartItemsList = document.getElementById('cartItemsList');
    const orderSummary = document.getElementById('orderSummary');
    const clearAllBtn = document.getElementById('clearAllBtn');
    const cartItemsCount = document.getElementById('cart-items-count');
    
    if (!cartData || !cartData.cartItems || cartData.cartItems.length === 0) {
        emptyCart.style.display = 'block';
        cartItemsList.style.display = 'none';
        orderSummary.style.display = 'none';
        clearAllBtn.style.display = 'none';
        cartItemsCount.textContent = '0';
        return;
    }
    
    emptyCart.style.display = 'none';
    cartItemsList.style.display = 'flex';
    orderSummary.style.display = 'block';
    clearAllBtn.style.display = 'block';
    cartItemsCount.textContent = cartData.cartItems.length;
    
    // Render cart items
    cartItemsList.innerHTML = cartData.cartItems.map(item => `
        <div class="cart-item" data-item-id="${item.id}">
            <div class="cart-item-image">
                <img src="${getOptimizedImage(item.service.imageURL)}" 
                     alt="${item.service.tenDichVu}" 
                     loading="lazy">
            </div>
            <div class="cart-item-details">
                <a href="/service_detail.html?id=${item.service.id}" class="cart-item-title">
                    ${item.service.tenDichVu}
                </a>
                <div class="cart-item-category">
                    <i class="fas fa-tag"></i>
                    ${item.service.category?.tenDanhMuc || 'Chưa phân loại'}
                </div>
                <div class="cart-item-price">
                    ${formatCurrency(item.service.giaCoBan)}
                </div>
            </div>
            <div class="cart-item-actions">
                <div class="quantity-controls">
                    <button class="quantity-btn decrease-btn" 
                            data-item-id="${item.id}" 
                            data-quantity="${item.quantity}"
                            ${item.quantity <= 1 ? 'disabled' : ''}>
                        <i class="fas fa-minus"></i>
                    </button>
                    <span class="quantity-value">${item.quantity}</span>
                    <button class="quantity-btn increase-btn" 
                            data-item-id="${item.id}" 
                            data-quantity="${item.quantity}">
                        <i class="fas fa-plus"></i>
                    </button>
                </div>
                <button class="remove-item-btn" data-item-id="${item.id}">
                    <i class="fas fa-trash-alt"></i>
                </button>
            </div>
        </div>
    `).join('');
    
    // Calculate totals
    const subtotal = cartData.cartItems.reduce((sum, item) => {
        return sum + (item.service.giaCoBan * item.quantity);
    }, 0);
    
    const discount = 0;
    const total = subtotal - discount;
    
    document.getElementById('subtotal').textContent = formatCurrency(subtotal);
    document.getElementById('discount').textContent = `-${formatCurrency(discount)}`;
    document.getElementById('totalAmount').textContent = formatCurrency(total);
    
    attachCartEventListeners();
};

const renderRecommendedServices = () => {
    const container = document.getElementById('recommendedServices');
    if (!recommendedServices || recommendedServices.length === 0) {
        document.getElementById('recommendedSection').style.display = 'none';
        return;
    }
    
    container.innerHTML = recommendedServices.map(service => `
        <div class="recommended-service-card">
            <a href="/service_detail.html?id=${service.id}" class="recommended-service-link">
                <div class="recommended-service-image">
                    <img src="${getOptimizedImage(service.imageURL)}" 
                         alt="${service.tenDichVu}" 
                         loading="lazy">
                </div>
                <div class="recommended-service-content">
                    <h4 class="recommended-service-title">${truncateText(service.tenDichVu, 6)}</h4>
                    <div class="recommended-service-price">${formatCurrency(service.giaCoBan)}</div>
                    <button class="btn-add-to-cart" 
                            data-service-id="${service.id}"
                            onclick="event.preventDefault(); addToCart(${service.id})">
                        <i class="fas fa-cart-plus"></i> Thêm vào giỏ
                    </button>
                </div>
            </a>
        </div>
    `).join('');
};

const updateHeaderCartCount = () => {
    const badge = document.getElementById('header-cart-count');
    if (badge) {
        const count = cartData?.cartItems?.length || 0;
        badge.textContent = count;
        badge.style.display = count > 0 ? 'flex' : 'none';
    }
};

const attachCartEventListeners = () => {
    // Decrease quantity
    document.querySelectorAll('.decrease-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const itemId = btn.dataset.itemId;
            const currentQty = parseInt(btn.dataset.quantity);
            if (currentQty > 1) {
                updateQuantity(itemId, currentQty - 1);
            }
        });
    });
    
    // Increase quantity
    document.querySelectorAll('.increase-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const itemId = btn.dataset.itemId;
            const currentQty = parseInt(btn.dataset.quantity);
            updateQuantity(itemId, currentQty + 1);
        });
    });
    
    document.querySelectorAll('.remove-item-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const itemId = btn.dataset.itemId;
            removeItem(itemId);
        });
    });
};

window.addToCart = async (serviceId) => {
    if (!checkAuth()) return;
    
    showLoading();
    try {
        const response = await fetch(`${API_BASE_URL}/cart/add`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({
                serviceId: serviceId,
                quantity: 1
            })
        });
        
        if (!response.ok) throw new Error('Failed to add to cart');
        
        await loadCart();
        showAlert('Đã thêm dịch vụ vào giỏ hàng');
    } catch (error) {
        console.error('Error adding to cart:', error);
        showAlert('Không thể thêm dịch vụ vào giỏ', 'error');
    } finally {
        hideLoading();
    }
};

// Checkout handler
const handleCheckout = () => {
    if (!cartData || !cartData.cartItems || cartData.cartItems.length === 0) {
        showAlert('Giỏ hàng trống', 'error');
        return;
    }
    
    // Store cart data in sessionStorage for checkout page
    sessionStorage.setItem('checkoutCart', JSON.stringify(cartData));
    window.location.href = '/checkout.html';
};

// Apply promo code
const applyPromoCode = async () => {
    const promoInput = document.getElementById('promoCodeInput');
    const code = promoInput.value.trim();
    
    if (!code) {
        showAlert('Vui lòng nhập mã giảm giá', 'error');
        return;
    }
    
    showLoading();
    try {
        showAlert('Chức năng đang được phát triển', 'error');
    } catch (error) {
        console.error('Error applying promo code:', error);
        showAlert('Mã giảm giá không hợp lệ', 'error');
    } finally {
        hideLoading();
    }
};

// Load authentication section
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
                  const avatar = user.avatarURL || '/assets/avatar/default-avatar.png';
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
                      const initialCount = localStorage.getItem('unreadNotificationCount') || 0;
                
                updateBadgeCount(initialCount);
                loadNotifications(token);
                setupNotificationClick();
              })
              .catch(() => {
                  localStorage.removeItem('jwtToken');
                  authSection.innerHTML = `<div class="auth-buttons"><a href="/login.html" class="btn btn-outline">Đăng nhập</a><a href="/register.html" class="btn btn-primary">Đăng ký</a></div>`;
              });
          } else {
              authSection.innerHTML = `<div class="auth-buttons"><a href="/login.html" class="btn btn-outline">Đăng nhập</a><a href="/register.html" class="btn btn-primary">Đăng ký</a></div>`;
          }
      }

window.logout = (event) => {
    event.preventDefault();
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('userRole');
    window.location.href = '/login.html';
};


const initBackToTop = () => {
    const btn = document.getElementById('backToTop');
    
    window.addEventListener('scroll', () => {
        if (window.scrollY > 300) {
            btn.style.display = 'flex';
        } else {
            btn.style.display = 'none';
        }
    });
    
    btn.addEventListener('click', () => {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
};

document.addEventListener('click', (e) => {
    if (e.target.closest('.close-alert')) {
        document.getElementById('success-alert').style.display = 'none';
    }
});


document.addEventListener('DOMContentLoaded', () => {
    loadAuthSection();
    loadCart();
    loadRecommendedServices();
    initBackToTop();
    
    // Event listeners
    document.getElementById('clearAllBtn')?.addEventListener('click', clearAllItems);
    document.getElementById('checkoutBtn')?.addEventListener('click', handleCheckout);
    document.getElementById('applyPromoBtn')?.addEventListener('click', applyPromoCode);
});
