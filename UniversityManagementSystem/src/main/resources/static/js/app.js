const role = window.currentRole;
let resource = role === 'ADMIN' ? 'admins' : role === 'TEACHER' ? 'teachers' : 'students';
let records = [];
let editingId = null;
const csrfToken = document.querySelector('meta[name="_csrf"]').content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
const tableBody = document.getElementById('userTable');
const backdrop = document.getElementById('modalBackdrop');

function endpoint() { return `/api/${resource}`; }
function escapeHtml(value = '') {
    return String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
}
function notify(message) {
    const toast = document.getElementById('toast'); toast.textContent = message;
    toast.classList.add('show'); setTimeout(() => toast.classList.remove('show'), 2600);
}
async function request(url, options = {}) {
    const response = await fetch(url, { ...options, headers: {
        'Content-Type': 'application/json', [csrfHeader]: csrfToken, ...(options.headers || {})
    }});
    if (!response.ok) {
        let message = `Request failed (${response.status})`;
        try { const data = await response.json(); message = data.detail || data.message || message; } catch (_) {}
        throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
}
async function loadRecords() {
    try {
        records = await request(endpoint()); renderTable();
        document.getElementById('recordCount').textContent = `${records.length} records`;
        const counts = {admins: 'adminCount', teachers: 'teacherCount', students: 'studentCount'};
        for (const [key, id] of Object.entries(counts)) {
            try { const data = key === resource ? records : await request(`/api/${key}`); document.getElementById(id).textContent = data.length; }
            catch (_) { document.getElementById(id).textContent = '—'; }
        }
    } catch (error) { notify(error.message); tableBody.innerHTML = `<tr><td colspan="5" class="empty-state">${escapeHtml(error.message)}</td></tr>`; }
}
function renderTable() {
    const query = document.getElementById('searchInput').value.toLowerCase();
    const filtered = records.filter(user => `${user.name} ${user.email} ${user.department || ''}`.toLowerCase().includes(query));
    document.getElementById('recordCount').textContent = `${filtered.length} records`;
    if (!filtered.length) { tableBody.innerHTML = '<tr><td colspan="5" class="empty-state">No records found. Add your first record.</td></tr>'; return; }
    tableBody.innerHTML = filtered.map(user => `<tr>
        <td><div class="person-cell"><div class="table-avatar">${escapeHtml(user.name.charAt(0).toUpperCase())}</div><strong>${escapeHtml(user.name)}</strong></div></td>
        <td>${escapeHtml(user.email)}</td><td>${escapeHtml(user.department || '—')}</td>
        <td><span class="role-pill ${user.role.toLowerCase()}">${user.role}</span></td>
        <td class="actions-cell"><button class="table-action edit-action" data-edit="${user.id}">Edit</button><button class="table-action delete-action" data-delete="${user.id}">Delete</button></td>
    </tr>`).join('');
}
function selectResource(next) {
    resource = next;
    document.querySelectorAll('.resource-link').forEach(button => button.classList.toggle('active', button.dataset.resource === next));
    document.getElementById('tableTitle').textContent = `Manage ${next}`;
    loadRecords();
}
document.querySelectorAll('.resource-link').forEach(button => button.addEventListener('click', () => selectResource(button.dataset.resource)));
document.getElementById('searchInput').addEventListener('input', renderTable);
document.getElementById('addButton').addEventListener('click', () => openModal());
function openModal(user = null) {
    editingId = user ? user.id : null;
    document.getElementById('modalTitle').textContent = user ? 'Edit user' : 'Add new user';
    document.getElementById('name').value = user?.name || '';
    document.getElementById('email').value = user?.email || '';
    document.getElementById('department').value = user?.department || '';
    document.getElementById('password').value = '';
    document.getElementById('password').required = !user;
    document.getElementById('passwordHint').textContent = user ? '(leave blank to keep current)' : '(required)';
    document.getElementById('formNote').textContent = user ? 'Leave password blank if you do not want to change it.' : 'Password will be securely hashed before saving.';
    backdrop.hidden = false;
}
function closeModal() { backdrop.hidden = true; document.getElementById('userForm').reset(); }
document.getElementById('closeModal').addEventListener('click', closeModal);
document.getElementById('cancelButton').addEventListener('click', closeModal);
backdrop.addEventListener('click', event => { if (event.target === backdrop) closeModal(); });
tableBody.addEventListener('click', async event => {
    const editId = event.target.dataset.edit;
    const deleteId = event.target.dataset.delete;
    if (editId) openModal(records.find(user => user.id === Number(editId)));
    if (deleteId && confirm('Are you sure you want to delete this record?')) {
        try { await request(`${endpoint()}/${deleteId}`, {method: 'DELETE'}); notify('Record deleted successfully.'); loadRecords(); }
        catch (error) { notify(error.message); }
    }
});
document.getElementById('userForm').addEventListener('submit', async event => {
    event.preventDefault();
    const payload = {name: document.getElementById('name').value.trim(), email: document.getElementById('email').value.trim(),
        department: document.getElementById('department').value.trim(), password: document.getElementById('password').value};
    try {
        await request(editingId ? `${endpoint()}/${editingId}` : endpoint(), {method: editingId ? 'PUT' : 'POST', body: JSON.stringify(payload)});
        closeModal(); notify(editingId ? 'Record updated successfully.' : 'Record created successfully.'); loadRecords();
    } catch (error) { notify(error.message); }
});
document.getElementById('tableTitle').textContent = `Manage ${resource}`;
loadRecords();
