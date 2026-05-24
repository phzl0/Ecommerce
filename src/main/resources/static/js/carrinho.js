function getCsrfToken() {
    const match = document.cookie.split(';')
        .map(c => c.trim())
        .find(c => c.startsWith('XSRF-TOKEN='));
    return match ? decodeURIComponent(match.split('=')[1]) : null;
}

function adicionarAoCarrinho(produtoId, quantidade = 1) {
    const token = getCsrfToken();

    fetch('/carrinho/adicionar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-XSRF-TOKEN': token
        },
        body: `produtoId=${produtoId}&quantidade=${quantidade}`
    })
        .then(r => r.json())
        .then(data => {
            if (data.sucesso) {
                atualizarContador(data.total);
                mostrarFeedback();
            } else {
                console.error('Erro:', data.erro);
            }
        })
        .catch(err => console.error('Erro na requisição:', err));
}

function atualizarContador(total) {
    const el = document.getElementById('cartCount');
    if (el) el.textContent = total;
}

function mostrarFeedback() {
    const div = document.createElement('div');
    div.textContent = '✓ Adicionado ao carrinho';
    div.style.cssText = `
        position:fixed;bottom:24px;right:24px;
        background:#27ae60;color:#fff;
        padding:12px 20px;border-radius:8px;
        font-size:14px;z-index:999;`;
    document.body.appendChild(div);
    setTimeout(() => div.remove(), 2200);
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.btn-add').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-id');
            if (id) adicionarAoCarrinho(id);
        });
    });
});