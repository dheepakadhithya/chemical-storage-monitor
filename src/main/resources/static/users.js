const usersGrid = document.getElementById('usersGrid');
const userSearchInput = document.getElementById('userSearchInput');
const addUserBtn = document.getElementById('add-user-btn');

// Add/Edit Modal
const userModal = document.getElementById('user-modal');
const modalClose = document.getElementById('modal-close');
const cancelUserBtn = document.getElementById('cancel-user-btn');
const userForm = document.getElementById('user-form');
const userModalTitle = document.getElementById('user-modal-title');
const submitUserBtn = document.getElementById('submit-user-btn');
const editUserIdInput = document.getElementById('edit-user-id');
const formUsername = document.getElementById('form-username');
const formPassword = document.getElementById('form-password');
const formRole = document.getElementById('form-role');
const formImageUrl = document.getElementById('form-image-url');
const passwordLabel = document.getElementById('password-label');
const passwordHint = document.getElementById('password-hint');

// View Modal
const viewUserModal = document.getElementById('view-user-modal');
const viewModalClose = document.getElementById('view-modal-close');
const closeViewUserBtn = document.getElementById('close-view-user-btn');
const viewUserImg = document.getElementById('view-user-img');
const viewUserUsername = document.getElementById('view-user-username');
const viewUserId = document.getElementById('view-user-id');
const viewUserRoleBadge = document.getElementById('view-user-role-badge');

let cachedUsers = [];

// ── Default Avatar Helpers ───────────────────────────────────────
function getDefaultUserImage(role, username) {
    const r = (role || 'USER').toUpperCase();
    if (r === 'ADMIN') {
        return 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80';
    } else if (r === 'OPERATOR') {
        return 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80';
    } else {
        return 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=80';
    }
}

// ── Sidebar Toggle Logic ────────────────────────────────────────
const sidebar = document.getElementById("sidebar");
const menuIcon = document.getElementById("menuIcon");
const sidebarBackdrop = document.getElementById("sidebar-backdrop");
const sidebarClose = document.getElementById("sidebar-close");

if (menuIcon && sidebar) {
    menuIcon.addEventListener('click', (e) => {
        e.stopPropagation();
        sidebar.classList.add("open");
        if (sidebarBackdrop) sidebarBackdrop.classList.add("show");
    });
}

function closeSidebar() {
    if (sidebar) sidebar.classList.remove("open");
    if (sidebarBackdrop) sidebarBackdrop.classList.remove("show");
}

if (sidebarClose) sidebarClose.addEventListener('click', closeSidebar);
if (sidebarBackdrop) sidebarBackdrop.addEventListener('click', closeSidebar);

document.addEventListener('click', (event) => {
    if (sidebar && sidebar.classList.contains("open")) {
        if (!sidebar.contains(event.target) && event.target !== menuIcon) {
            closeSidebar();
        }
    }
});

// ── Fetch & Render Users ────────────────────────────────────────
async function loadUsers() {
    try {
        const response = await fetch('/api/users');
        if (response.status === 403) {
            window.location.href = '/index.html?error=access_denied';
            return;
        }
        if (!response.ok) return;

        cachedUsers = await response.json();
        renderUserCatalog(cachedUsers);

    } catch (err) {
        console.error('Error fetching users:', err);
    }
}

function renderUserCatalog(usersToRender = cachedUsers) {
    usersGrid.innerHTML = '';

    if (!usersToRender || usersToRender.length === 0) {
        usersGrid.innerHTML = '<p style="color: #1e293b; font-weight: 500; padding: 1rem;">No user accounts found matching your query.</p>';
        return;
    }

    usersToRender.forEach(u => {
        const card = document.createElement('div');
        card.className = 'user-card';

        const role = (u.role || 'USER').toUpperCase();
        let roleClass = 'role-viewer';

        if (role === 'ADMIN') {
            roleClass = 'role-admin';
        } else if (role === 'OPERATOR') {
            roleClass = 'role-operator';
        }

        const isPrimaryAdmin = (u.username || '').toLowerCase() === 'admin';
        const userImg = u.imageUrl && u.imageUrl.trim().length > 0
            ? u.imageUrl
            : getDefaultUserImage(role, u.username);

        // Name and role come directly on top of the user image (matching the "Available" badge screenshot)
        card.innerHTML = `
            <div>
                <div class="user-card-image-wrap">
                    <img src="${userImg}" alt="${u.username}" class="user-card-img" onerror="this.src='https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500'">
                    
                    <!-- Floating Role on top of user image -->
                    <div class="user-image-top-badges">
                        <span class="badge-status-pill ${roleClass}">
                            ${role}
                        </span>
                    </div>

                    <!-- Name and ID banner on top of image bottom -->
                    <div class="user-image-overlay-bottom">
                        <h3 class="user-overlay-name" title="${u.username}">${u.username}</h3>
                        <span class="user-overlay-id">#USER-00${u.id}</span>
                    </div>
                </div>
            </div>

            <!-- Action Buttons: View, Edit, Delete (Matching products page) -->
            <div class="user-card-actions">
                <button class="viewBtn" onclick="openViewUserModal(${u.id})">
                    <i class="fa-solid fa-eye"></i> View
                </button>
                <button class="editBtn" onclick="openEditUserModal(${u.id})">
                    <i class="fa-solid fa-pen-to-square"></i> Edit
                </button>
                <button class="deleteBtn" ${isPrimaryAdmin ? 'disabled title="Primary admin cannot be deleted"' : ''} onclick="deleteUser(${u.id}, '${u.username}')">
                    <i class="fa-solid fa-trash-can"></i> Delete
                </button>
            </div>
        `;
        usersGrid.appendChild(card);
    });
}

// ── Search Users Filter ─────────────────────────────────────────
if (userSearchInput) {
    userSearchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase().trim();
        const filtered = cachedUsers.filter(u => {
            const name = (u.username || '').toLowerCase();
            const role = (u.role || '').toLowerCase();
            return name.includes(query) || role.includes(query);
        });
        renderUserCatalog(filtered);
    });
}

// ── View User Modal ─────────────────────────────────────────────
window.openViewUserModal = function(id) {
    const user = cachedUsers.find(u => u.id === id);
    if (!user) return;

    const role = (user.role || 'USER').toUpperCase();
    let roleClass = 'role-viewer';

    if (role === 'ADMIN') {
        roleClass = 'role-admin';
    } else if (role === 'OPERATOR') {
        roleClass = 'role-operator';
    }

    const userImg = user.imageUrl && user.imageUrl.trim().length > 0
        ? user.imageUrl
        : getDefaultUserImage(role, user.username);

    if (viewUserImg) {
        viewUserImg.src = userImg;
        viewUserImg.onerror = () => { viewUserImg.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500'; };
    }
    if (viewUserUsername) viewUserUsername.textContent = user.username;
    if (viewUserId) viewUserId.textContent = `User ID: #USER-00${user.id}`;
    if (viewUserRoleBadge) {
        viewUserRoleBadge.innerHTML = `<span class="badge-status-pill ${roleClass}">${role}</span>`;
    }

    // Dynamic Account Status: only active session user shows Active, others show Offline
    const currentUsername = (window.currentUser?.username || '').toLowerCase();
    const isCurrentSessionUser = (user.username || '').toLowerCase() === currentUsername;
    const viewUserStatus = document.getElementById('view-user-status');
    if (viewUserStatus) {
        if (isCurrentSessionUser) {
            viewUserStatus.innerHTML = `
                <span style="color: #16a34a; font-size: 0.88rem; font-weight: 600; display: inline-flex; align-items: center; gap: 6px;">
                    <i class="fa-solid fa-circle-check"></i> Active (Current Session)
                </span>
            `;
        } else {
            viewUserStatus.innerHTML = `
                <span style="color: #1e293b; font-size: 0.88rem; font-weight: 600; display: inline-flex; align-items: center; gap: 6px;">
                    <i class="fa-solid fa-circle" style="font-size: 0.6rem; color: #475569;"></i> Offline
                </span>
            `;
        }
    }

    viewUserModal.style.display = 'block';
};

function closeViewModal() {
    if (viewUserModal) viewUserModal.style.display = 'none';
}
if (viewModalClose) viewModalClose.onclick = closeViewModal;
if (closeViewUserBtn) closeViewUserBtn.onclick = closeViewModal;

// ── Edit User Modal ─────────────────────────────────────────────
window.openEditUserModal = function(id) {
    const user = cachedUsers.find(u => u.id === id);
    if (!user) return;

    editUserIdInput.value = user.id;
    formUsername.value = user.username;
    formPassword.value = '';
    formPassword.required = false;
    formRole.value = (user.role || 'OPERATOR').toUpperCase();
    if (formImageUrl) formImageUrl.value = user.imageUrl || '';

    passwordLabel.textContent = 'Change Password (Optional)';
    passwordHint.style.display = 'block';
    userModalTitle.innerHTML = '<i class="fa-solid fa-user-pen"></i> Edit User Account';
    submitUserBtn.textContent = 'Update Account';

    userModal.style.display = 'block';
};

// ── Add User Modal ──────────────────────────────────────────────
if (addUserBtn) {
    addUserBtn.onclick = () => {
        editUserIdInput.value = '';
        userForm.reset();
        formPassword.required = true;
        if (formImageUrl) formImageUrl.value = '';
        passwordLabel.textContent = 'Password *';
        passwordHint.style.display = 'none';
        userModalTitle.innerHTML = '<i class="fa-solid fa-user-plus"></i> Add System User';
        submitUserBtn.textContent = 'Create Account';
        userModal.style.display = 'block';
    };
}

function closeUserModal() {
    if (userModal) userModal.style.display = 'none';
    if (userForm) userForm.reset();
    editUserIdInput.value = '';
}

if (modalClose) modalClose.onclick = closeUserModal;
if (cancelUserBtn) cancelUserBtn.onclick = closeUserModal;

window.addEventListener('click', (e) => {
    if (e.target === userModal) closeUserModal();
    if (e.target === viewUserModal) closeViewModal();
});

// ── Submit User Form (Add / Edit) ───────────────────────────────
if (userForm) {
    userForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const editId = editUserIdInput.value;
        const username = formUsername.value.trim();
        const password = formPassword.value.trim();
        const role = formRole.value;
        const imageUrl = formImageUrl ? formImageUrl.value.trim() : '';

        const payload = { username, role, imageUrl };
        if (password) {
            payload.password = password;
        }

        try {
            let response;
            if (editId) {
                // Update
                response = await fetch(`/api/users/${editId}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
            } else {
                // Create
                if (!password) {
                    alert('Password is required for new accounts.');
                    return;
                }
                response = await fetch('/api/users', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
            }

            if (response.ok) {
                closeUserModal();
                loadUsers();
            } else {
                const errData = await response.json();
                alert(errData.error || 'Failed to save user account.');
            }
        } catch (err) {
            console.error('Error saving user:', err);
        }
    });
}

// ── Delete User ─────────────────────────────────────────────────
window.deleteUser = async function(id, username) {
    if (!confirm(`Are you sure you want to delete user "${username}"?`)) return;

    try {
        const response = await fetch(`/api/users/${id}`, { method: 'DELETE' });
        if (response.ok) {
            loadUsers();
        } else {
            const errData = await response.json();
            alert(errData.error || 'Failed to delete user.');
        }
    } catch (err) {
        console.error(err);
    }
};

// Initial Load
loadUsers();
