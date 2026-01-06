function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container') || createToastContainer();
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<div style="padding:15px 20px; border-radius:5px; color:white; background:${type==='success'?'#28a745':'#dc3545'}; box-shadow:0 3px 10px rgba(0,0,0,0.2); margin-bottom:10px;">${message}</div>`;
    container.appendChild(toast);
    setTimeout(() => toast.querySelector('div').style.opacity = '1', 10);
    setTimeout(() => {
        toast.querySelector('div').style.opacity = '0';
        setTimeout(() => toast.remove(), 500);
    }, 4000);
}

function createToastContainer() {
    const div = document.createElement('div');
    div.id = 'toast-container';
    div.style.cssText = 'position:fixed; top:20px; right:20px; z-index:1000;';
    document.body.appendChild(div);
    return div;
}