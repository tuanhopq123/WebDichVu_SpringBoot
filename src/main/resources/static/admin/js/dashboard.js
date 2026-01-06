import { showToast } from './toast.js';
import { fetchWithAuthCheck } from './api.js';

let revenueChart, topServicesChart;

async function loadDashboardData(filterType, filterValue) {
    try {
        
        const params = new URLSearchParams();
        params.append('type', filterType);
        if (filterValue) params.append('value', filterValue);

        const response = await fetchWithAuthCheck(`/api/admin/dashboard/stats?${params}`);
        const data = await response.json();

        updateTotalUsers(data.totalUsers);
        updateRevenueChart(data.revenueData, filterType);
        updateTopServicesChart(data.topServices);
        updateTotalRevenue(data.totalRevenue);

    } catch (error) {
        showToast('Lỗi tải dữ liệu thống kê: ' + error.message, 'error');
    }
}

function updateTotalUsers(count) {
    const el = document.getElementById('total-users');
    if (el) el.textContent = Number(count).toLocaleString('vi-VN');
}

function updateRevenueChart(data, filterType) {
    const ctx = document.getElementById('revenueChart').getContext('2d');
    const labels = data.map(d => d.label);
    const values = data.map(d => Number(d.total));
    // === PHẦN SỬA ĐỔI BẮT ĐẦU ===
    // 1. Tự tính tổng doanh thu từ mảng 'values'
    const totalRevenue = values.reduce((sum, currentValue) => sum + currentValue, 0);
    // 2. Định dạng chuỗi (ví dụ: 1.234.567đ)
    const formattedTotal = totalRevenue.toLocaleString('vi-VN') + 'đ';
    // 3. Tạo nhãn mới
    const chartLabel = `Tổng Doanh Thu: ${formattedTotal}`;
    // === PHẦN SỬA ĐỔI KẾT THÚC ===
    if (revenueChart) revenueChart.destroy();
    revenueChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: chartLabel, // <-- SỬA Ở ĐÂY (thay vì 'Doanh thu (VNĐ)')
                data: values,
                backgroundColor: 'rgba(0, 123, 255, 0.6)',
                borderColor: 'rgba(0, 123, 255, 1)',
                borderWidth: 1
            }]
        },
        options: {
            // (Tất cả options của bạn giữ nguyên)
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'top' },
                tooltip: {
                    callbacks: {
                        // Sửa lại tooltip để nó hiển thị đúng nhãn của cột
                        label: ctx => `Doanh thu: ${ctx.parsed.y.toLocaleString('vi-VN')}đ`
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: value => value.toLocaleString('vi-VN') + 'đ'
                    }
                }
            }
        }
    });
}

function updateTopServicesChart(services) {
    const ctx = document.getElementById('topServicesChart').getContext('2d');
    const labels = services.map(s => s.tenDichVu);
    const data = services.map(s => Number(s.soLanDat));
    const colors = services.map(() => `hsl(${Math.random() * 360}, 70%, 60%)`);

    if (topServicesChart) topServicesChart.destroy();

    topServicesChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels.map((name, i) => `${name} (${data[i]} lần)`),
            datasets: [{
                data: data,
                backgroundColor: colors,
                borderColor: '#fff',
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'right' },
                tooltip: {
                    callbacks: {
                        label: ctx => `${ctx.label}`
                    }
                }
            }
        }
    });
}

function updateTotalRevenue(total) {
    const el = document.getElementById('total-revenue');
    if (el) {
        // Định dạng số (ví dụ: 1,234,567đ)
        el.textContent = Number(total).toLocaleString('vi-VN') + 'đ';
    }
}

// BƯỚC 4: EXPORT hàm init chính
// (Hàm này sẽ được gọi bởi utils.js SAU KHI HTML được tải)

export function initDashboardPage() {
    console.log("DEBUG: initDashboardPage() đã chạy.");

    // === XỬ LÝ BỘ LỌC (Gắn listener) ===
    const filterTypeEl = document.getElementById('filter-type');
    const dateEl = document.getElementById('filter-date');
    const monthEl = document.getElementById('filter-month');
    const yearEl = document.getElementById('filter-year');
    const applyBtn = document.getElementById('apply-filter');

    // Bắt buộc phải gắn listener ở đây, sau khi HTML đã được chèn
    filterTypeEl.addEventListener('change', () => {
        const type = filterTypeEl.value;
        dateEl.style.display = type === 'day' ? 'block' : 'none';
        monthEl.style.display = type === 'month' ? 'block' : 'none';
        yearEl.style.display = type === 'year' ? 'block' : 'none';
    });

    applyBtn.addEventListener('click', () => {
        const type = filterTypeEl.value;
        let value = '';

        if (type === 'day') value = dateEl.value;
        else if (type === 'month') value = monthEl.value;
        else if (type === 'year') value = yearEl.value;

        if (!value && type !== 'year') {
            showToast('Vui lòng chọn giá trị để lọc.', 'error');
            return;
        }

        // Gọi hàm helper (đã được định nghĩa ở BƯỚC 3)
        loadDashboardData(type, value);
    });

    // === KHỞI TẠO MẶC ĐỊNH: Tháng hiện tại ===
    const today = new Date();
    const currentMonth = today.toISOString().slice(0, 7); // YYYY-MM

    filterTypeEl.value = 'month';
    monthEl.value = currentMonth;
    monthEl.style.display = 'block';
    dateEl.style.display = 'none'; 
    yearEl.style.display = 'none'; 

    // Gọi hàm helper (đã được định nghĩa ở BƯỚC 3)
    loadDashboardData('month', currentMonth);
}