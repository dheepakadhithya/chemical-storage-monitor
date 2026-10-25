// ── Header User Profile Loader ──────────────────────────────────
// Automatically fetches logged-in user and populates header profile
async function initHeaderUserProfile() {
    const avatarEl = document.getElementById('headerUserAvatar');
    const nameEl = document.getElementById('headerUserName');
    const roleEl = document.getElementById('headerUserRole');

    try {
        const res = await fetch('/api/users/current');
        if (res.ok) {
            const user = await res.json();
            const role = (user.role || 'USER').toUpperCase();
            window.currentUser = user;
            window.currentUserRole = role;
            
            if (nameEl) {
                nameEl.textContent = user.username || 'User';
            }
            if (roleEl) {
                roleEl.textContent = role;
            }
            if (avatarEl) {
                let img = user.imageUrl;
                if (!img || img.trim().length === 0) {
                    if (role === 'ADMIN') {
                        img = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80';
                    } else if (role === 'OPERATOR') {
                        img = 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80';
                    } else {
                        img = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=80';
                    }
                }
                avatarEl.src = img;
                avatarEl.onerror = () => {
                    avatarEl.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500';
                };
            }

            // Role-based navigation restriction: hide Users menu item for non-admins
            if (role !== 'ADMIN') {
                const usersNavLi = document.querySelector('#sidebar a[href*="users.html"]')?.closest('li');
                if (usersNavLi) {
                    usersNavLi.style.display = 'none';
                }
            }

            // Dispatch global event for other scripts (e.g. chemicals.js)
            document.dispatchEvent(new CustomEvent('user-profile-loaded', { detail: user }));
        } else {
            // Default fallback if endpoint returns 401 or non-200
            if (avatarEl && !avatarEl.src) {
                avatarEl.src = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80';
            }
        }
    } catch (err) {
        console.error('Error fetching current user profile for header:', err);
        if (avatarEl && !avatarEl.src) {
            avatarEl.src = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80';
        }
    }

    // Check for access_denied query parameter
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('error') === 'access_denied') {
        const alertBox = document.createElement('div');
        alertBox.style.cssText = 'position: fixed; top: 75px; right: 20px; z-index: 99999; background: #fee2e2; border-left: 4px solid #ef4444; color: #991b1b; padding: 12px 18px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); font-size: 0.9rem; font-weight: 600; display: flex; align-items: center; gap: 10px;';
        alertBox.innerHTML = '<i class="fa-solid fa-triangle-exclamation" style="font-size: 1.2rem;"></i> Access Denied: You do not have administrative privileges to access that section.';
        document.body.appendChild(alertBox);
        setTimeout(() => alertBox.remove(), 6000);
    }
}

// Initialize on DOM load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initHeaderUserProfile);
} else {
    initHeaderUserProfile();
}

