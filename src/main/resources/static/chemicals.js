const chemicalsGrid = document.getElementById('chemicals-grid');
const addChemBtn = document.getElementById('add-chem-btn');
const chemicalSearchInput = document.getElementById('chemicalSearchInput');
const chemModal = document.getElementById('chem-modal');
const modalClose = document.getElementById('modal-close');
const cancelChemBtn = document.getElementById('cancel-chem-btn');
const chemForm = document.getElementById('chem-form');
const modalTitle = document.getElementById('modal-title');
const submitChemBtn = document.getElementById('submit-chem-btn');
const editChemIdInput = document.getElementById('edit-chem-id');

let cachedChemicals = [];
let chemicalSearchQuery = '';

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

// ── User Role Resolution & Role-Based UI ────────────────────────
async function getCurrentUserRole() {
    if (window.currentUserRole) return window.currentUserRole;
    try {
        const res = await fetch('/api/users/current');
        if (res.ok) {
            const data = await res.json();
            window.currentUserRole = (data.role || 'USER').toUpperCase();
            return window.currentUserRole;
        }
    } catch (e) {
        console.error('Error fetching current user role:', e);
    }
    return 'USER';
}

// ── Fetch & Render Chemicals ────────────────────────────────────
async function loadChemicals() {
    try {
        const role = await getCurrentUserRole();
        const isAdmin = role === 'ADMIN';
        const isOperator = role === 'OPERATOR';
        const isViewer = role === 'VIEWER';

        // Restrict "Add Chemical" button for viewers
        if (addChemBtn) {
            addChemBtn.style.display = isViewer ? 'none' : 'inline-flex';
        }

        const response = await fetch('/api/chemicals');
        if (!response.ok) return;

        cachedChemicals = await response.json();
        chemicalsGrid.innerHTML = '';

        if (!cachedChemicals || cachedChemicals.length === 0) {
            chemicalsGrid.innerHTML = '<p style="color: #1e293b; font-weight: 500;">No chemical profiles registered yet. Click "Add Chemical" above.</p>';
            return;
        }

        cachedChemicals.forEach(chem => {
            const card = document.createElement('div');
            card.className = `chem-card ${chem.active ? 'active-in-cabinet' : ''}`;
            card.dataset.chemId = chem.id;

            const cleanImg = (chem.imageUrl && chem.imageUrl.trim().length > 0)
                ? chem.imageUrl.trim()
                : 'https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=400';
            const imgHtml = `<div class="chem-card-thumb"><img src="${cleanImg}" alt="${chem.name}" onerror="this.onerror=null; this.src='https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=400';"></div>`;

            let activePill;
            if (chem.active) {
                activePill = `<span class="badge-active-pill" style="font-size: 0.72rem; padding: 3px 8px;"><i class="fa-solid fa-circle-dot"></i> Monitored in Cabinet</span>`;
            } else if (!isViewer) {
                // Admin and Operator can set active
                activePill = `<button onclick="setActiveMonitoredChem(${chem.id})" style="border: 1px solid #333333; background: #f8fafc; color: #333333; font-size: 0.72rem; font-weight: 600; padding: 3px 8px; border-radius: 9999px; cursor: pointer;" title="Set as current chemical in physical cabinet"><i class="fa-solid fa-box-archive"></i> Set Active</button>`;
            } else {
                // Viewer cannot change active cabinet chemical
                activePill = `<span style="font-size: 0.72rem; color: #334155; font-weight: 600; padding: 3px 6px;"><i class="fa-solid fa-box-archive"></i> Inactive</span>`;
            }

            // Role-based actions
            let actionButtons = `
                <!-- View Button (All roles can view) -->
                <a href="index.html#chem-${chem.id}" class="btn-view-action" title="Open live safety dashboard">
                    <i class="fa-solid fa-gauge-high"></i> View
                </a>
            `;

            // Edit button (Admin & Operator only)
            if (!isViewer) {
                actionButtons += `
                    <button class="btn-edit-action" onclick="openEditModal(${chem.id})" title="Edit thresholds and details">
                        <i class="fa-solid fa-pen-to-square"></i> Edit
                    </button>
                `;
            }

            // Delete button (Admin only)
            if (isAdmin) {
                actionButtons += `
                    <button class="btn-delete-chem" title="Delete chemical" onclick="deleteChemical(${chem.id}, '${chem.name}')">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                `;
            }

            card.innerHTML = `
                <div>
                    ${imgHtml}
                    <div class="chem-card-header" style="display: flex; justify-content: space-between; align-items: flex-start; gap: 0.5rem;">
                        <div>
                            <h3 class="chem-name">${chem.name}</h3>
                            <span class="chem-formula">${chem.formula || 'N/A'}</span>
                        </div>
                        ${activePill}
                    </div>
                    <div style="margin: 0.4rem 0;">
                        <span class="badge badge-hazard">${chem.hazardCategory || 'General Storage'}</span>
                    </div>
                    <div class="chem-limits">
                        <div>
                            <div class="limit-box-title">Max Safe Temp</div>
                            <div class="limit-box-val" style="color: #e11d48;">${chem.maxTemperature} °C</div>
                        </div>
                        <div>
                            <div class="limit-box-title">Max Safe Humidity</div>
                            <div class="limit-box-val" style="color: #0284c7;">${chem.maxHumidity} %</div>
                        </div>
                    </div>
                    <p class="chem-instructions">${chem.storageInstructions || 'Standard controlled storage guidelines apply.'}</p>
                </div>
                <div class="chem-card-actions">
                    ${actionButtons}
                </div>
            `;
            chemicalsGrid.appendChild(card);
        });

        applyChemicalSearchFilter();

    } catch (error) {
        console.error('Error fetching chemicals:', error);
    }
}

// ── Search Chemicals Filter ─────────────────────────────────────
function applyChemicalSearchFilter() {
    if (!chemicalsGrid) return;
    const cards = chemicalsGrid.querySelectorAll('.chem-card');
    let visibleCount = 0;
    const query = (chemicalSearchQuery || '').toLowerCase().trim();

    cards.forEach(card => {
        const chemId = parseInt(card.dataset.chemId);
        const chem = (cachedChemicals || []).find(c => Number(c.id) === Number(chemId));

        const cardText = (card.textContent || '').toLowerCase();
        const chemName = (chem?.name || '').toLowerCase();
        const chemFormula = (chem?.formula || '').toLowerCase();
        const chemHazard = (chem?.hazardCategory || '').toLowerCase();
        const chemInstructions = (chem?.storageInstructions || '').toLowerCase();

        const matches = !query ||
            cardText.includes(query) ||
            chemName.includes(query) ||
            chemFormula.includes(query) ||
            chemHazard.includes(query) ||
            chemInstructions.includes(query);

        card.style.display = matches ? 'flex' : 'none';
        if (matches) visibleCount++;
    });

    let noResultsMsg = document.getElementById('chem-no-search-results');
    if (cards.length > 0 && visibleCount === 0 && query) {
        if (!noResultsMsg) {
            noResultsMsg = document.createElement('p');
            noResultsMsg.id = 'chem-no-search-results';
            noResultsMsg.style.cssText = 'color: #1e293b; font-weight: 500; padding: 1.5rem; grid-column: 1 / -1; margin: 0;';
            chemicalsGrid.appendChild(noResultsMsg);
        }
        noResultsMsg.textContent = `No chemicals found matching "${chemicalSearchQuery}".`;
        noResultsMsg.style.display = 'block';
    } else if (noResultsMsg) {
        noResultsMsg.style.display = 'none';
    }
}

if (chemicalSearchInput) {
    chemicalSearchInput.addEventListener('input', (e) => {
        chemicalSearchQuery = e.target.value.toLowerCase().trim();
        applyChemicalSearchFilter();
    });
}

// ── Open Edit Modal ─────────────────────────────────────────────
window.openEditModal = function(id) {
    if (window.currentUserRole === 'VIEWER') {
        alert('Access Denied: Viewers cannot edit chemical profiles.');
        return;
    }
    const chem = cachedChemicals.find(c => c.id === id);
    if (!chem) return;

    editChemIdInput.value = chem.id;
    document.getElementById('chem-name').value = chem.name;
    document.getElementById('chem-formula').value = chem.formula || '';
    document.getElementById('chem-hazard').value = chem.hazardCategory || '';
    document.getElementById('chem-max-temp').value = chem.maxTemperature;
    document.getElementById('chem-max-hum').value = chem.maxHumidity;
    document.getElementById('chem-image-url').value = chem.imageUrl || '';
    document.getElementById('chem-instructions').value = chem.storageInstructions || '';

    modalTitle.innerHTML = '<i class="fa-solid fa-pen-to-square"></i> Edit Chemical Profile';
    submitChemBtn.textContent = 'Update Profile';
    chemModal.style.display = 'block';
};

// ── Open Add Modal ──────────────────────────────────────────────
if (addChemBtn) {
    addChemBtn.onclick = () => {
        if (window.currentUserRole === 'VIEWER') {
            alert('Access Denied: Viewers cannot add chemical profiles.');
            return;
        }
        editChemIdInput.value = '';
        chemForm.reset();
        modalTitle.innerHTML = '<i class="fa-solid fa-flask"></i> Add Chemical Profile';
        submitChemBtn.textContent = 'Save Profile';
        chemModal.style.display = 'block';
    };
}

function hideModal() {
    if (chemModal) chemModal.style.display = 'none';
    if (chemForm) chemForm.reset();
    editChemIdInput.value = '';
}

if (modalClose) modalClose.onclick = hideModal;
if (cancelChemBtn) cancelChemBtn.onclick = hideModal;

window.addEventListener('click', (e) => {
    if (e.target === chemModal) hideModal();
});

// ── Save or Update Chemical ─────────────────────────────────────
if (chemForm) {
    chemForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const editId = editChemIdInput.value;
        const payload = {
            name: document.getElementById('chem-name').value.trim(),
            formula: document.getElementById('chem-formula').value.trim(),
            hazardCategory: document.getElementById('chem-hazard').value.trim(),
            maxTemperature: parseFloat(document.getElementById('chem-max-temp').value),
            maxHumidity: parseFloat(document.getElementById('chem-max-hum').value),
            imageUrl: document.getElementById('chem-image-url').value.trim(),
            storageInstructions: document.getElementById('chem-instructions').value.trim()
        };

        try {
            let response;
            if (editId) {
                // Update existing chemical
                response = await fetch(`/api/chemicals/${editId}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
            } else {
                // Create new chemical
                response = await fetch('/api/chemicals', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
            }

            if (response.status === 403) {
                alert('Access Denied: You do not have permission to modify chemicals.');
                return;
            }

            if (response.ok) {
                hideModal();
                loadChemicals();
            } else {
                const errData = await response.json().catch(() => ({}));
                alert(errData.error || 'Failed to save chemical profile.');
            }
        } catch (err) {
            console.error('Error saving chemical:', err);
        }
    });
}

// ── Delete Chemical ─────────────────────────────────────────────
window.deleteChemical = async function(id, name) {
    if (window.currentUserRole !== 'ADMIN') {
        alert('Access Denied: Only administrators can delete chemical profiles.');
        return;
    }

    if (!confirm(`Are you sure you want to delete profile for "${name}"?`)) return;

    try {
        const response = await fetch(`/api/chemicals/${id}`, { method: 'DELETE' });
        if (response.status === 403) {
            alert('Access Denied: Only administrators have permission to delete chemicals.');
            return;
        }
        if (response.ok) {
            loadChemicals();
        } else {
            const errData = await response.json().catch(() => ({}));
            alert(errData.error || 'Failed to delete chemical profile.');
        }
    } catch (err) {
        console.error(err);
    }
};

// ── Set Active Monitored Chemical ───────────────────────────────
window.setActiveMonitoredChem = async function(id) {
    if (window.currentUserRole === 'VIEWER') {
        alert('Access Denied: Viewers cannot change the active monitored chemical.');
        return;
    }

    try {
        const response = await fetch(`/api/chemicals/active/${id}`, { method: 'POST' });
        if (response.status === 403) {
            alert('Access Denied: You do not have permission to change the active chemical.');
            return;
        }
        if (response.ok) {
            loadChemicals();
        } else {
            const errData = await response.json().catch(() => ({}));
            alert(errData.error || 'Failed to set active chemical in cabinet.');
        }
    } catch (err) {
        console.error(err);
    }
};

// Listen for profile load event from header.js
document.addEventListener('user-profile-loaded', () => {
    loadChemicals();
});

// Initial Load
loadChemicals();

