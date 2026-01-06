function showToast(message, type) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toasts = container.querySelectorAll('.toast');
    if (toasts.length >= 3) {
        toasts[0].remove();
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = message;
    container.appendChild(toast);
    setTimeout(() => { toast.classList.add('show'); }, 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => { toast.remove(); }, 500);
    }, 4000);
    console.log(`DEBUG: showToast - ${type}: ${message}`);
}

export { showToast };