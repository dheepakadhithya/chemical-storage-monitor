// ── DOM Elements ────────────────────────────────────────────────
const overviewView = document.getElementById('overview-view');
const singleChemicalView = document.getElementById('single-chemical-view');
const chemicalsOverviewGrid = document.getElementById('chemicals-overview-grid');
const dashboardSearchInput = document.getElementById('dashboardSearchInput');
const pageTitle = document.getElementById('page-title');
const btnBackOverview = document.getElementById('btn-back-overview');

// Single Chemical Elements
const singleChemName = document.getElementById('single-chem-name');
const singleChemFormula = document.getElementById('single-chem-formula');
const singleChemHazard = document.getElementById('single-chem-hazard');
const singleChemActiveBadge = document.getElementById('single-chem-active-badge');
const btnSetActiveCabinet = document.getElementById('btn-set-active-cabinet');
const alertChemTitle = document.getElementById('alert-chem-title');
const historyModalChemTitle = document.getElementById('history-modal-chem-title');

// Metric Elements
const tempValEl = document.getElementById('temp-val');
const humValEl = document.getElementById('hum-val');
const tempStatusEl = document.getElementById('temp-status');
const humStatusEl = document.getElementById('hum-status');
const tempCardEl = document.getElementById('temp-card');
const humCardEl = document.getElementById('hum-card');
const tempHintEl = document.getElementById('temp-threshold-hint');
const humHintEl = document.getElementById('hum-threshold-hint');

// Ultrasonic Radar Elements
const usValEl = document.getElementById('us-val');
const usStatusEl = document.getElementById('us-status');
const usCardEl = document.getElementById('us-card');
const usHintEl = document.getElementById('us-threshold-hint');

// Cabinet Lock Actuator Elements
const lockCardEl = document.getElementById('actuator-lock-card');
const lockStatusPill = document.getElementById('lock-status-pill');
const lockDesc = document.getElementById('lock-desc');
const lockIcon = document.getElementById('lock-icon');
const btnLockArm = document.getElementById('btn-lock-arm');
const btnLockDisarm = document.getElementById('btn-lock-disarm');
const distanceThresholdInput = document.getElementById('distance-threshold-input');
const distanceThresholdVal = document.getElementById('distance-threshold-val');

const alertsTbody = document.getElementById('alerts-tbody');
const lastUpdatedEl = document.getElementById('last-updated-text');

// History Modal Elements
const historyBtn = document.getElementById('view-history-btn');
const historyModal = document.getElementById('history-modal');
const closeBtn = document.querySelector('.close-btn');
const historyTbody = document.getElementById('history-tbody');

// State
let chemicalsList = [];
let selectedChemical = null;
let pollTimerReadings = null;
let pollTimerAlerts = null;
let pollTimerOverview = null;
let pollTimerControls = null;
let currentDeviceModes = { fan: 'NORMAL', buzzer: 'NORMAL', light: 'NORMAL' };
let currentCabinetLock = 'LOCKED';
let currentDistanceThreshold = 10.0;
let dashboardSearchQuery = '';

// ── Update Metric Card Styles ───────────────────────────────────
function updateMetric(valEl, statusEl, cardEl, value, unit, isDanger) {
    if (value === null || value === undefined || isNaN(value)) {
        valEl.textContent = `-- ${unit}`;
        valEl.classList.remove('danger-text');
        if (cardEl) cardEl.classList.remove('card-danger', 'card-safe');
        if (statusEl) {
            statusEl.className = 'badge badge-neutral';
            statusEl.textContent = '--';
        }
        return;
    }

    valEl.textContent = `${Number(value).toFixed(1)} ${unit}`;

    if (isDanger) {
        valEl.classList.add('danger-text');
        if (cardEl) {
            cardEl.classList.add('card-danger');
            cardEl.classList.remove('card-safe');
        }
        if (statusEl) {
            statusEl.className = 'badge badge-danger';
            statusEl.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> DANGER';
        }
    } else {
        valEl.classList.remove('danger-text');
        if (cardEl) {
            cardEl.classList.add('card-safe');
            cardEl.classList.remove('card-danger');
        }
        if (statusEl) {
            statusEl.className = 'badge badge-safe';
            statusEl.innerHTML = '<i class="fa-solid fa-circle-check"></i> SAFE';
        }
    }
}

// ── VIEW 1: Overview Grid with "View" Buttons ────────────────────
async function loadOverview() {
    try {
        const [chemRes, latestRes] = await Promise.all([
            fetch('/api/chemicals'),
            fetch('/api/readings/all-latest')
        ]);

        if (!chemRes.ok) return;

        chemicalsList = await chemRes.json();
        const latestMap = latestRes.ok ? await latestRes.json() : {};

        if (!chemicalsList || chemicalsList.length === 0) {
            chemicalsOverviewGrid.innerHTML = '<p style="color: #1e293b;">No chemicals registered yet.</p>';
            return;
        }

        const existingCardIds = new Set(
            Array.from(chemicalsOverviewGrid.querySelectorAll('.dash-chem-card'))
                .map(el => parseInt(el.dataset.chemId))
                .filter(id => !isNaN(id))
        );

        const currentIds = new Set(chemicalsList.map(c => c.id));
        const structureChanged = existingCardIds.size !== currentIds.size || 
            [...currentIds].some(id => !existingCardIds.has(id));

        // If card structure or count changed, build full cards once
        if (structureChanged || chemicalsOverviewGrid.children.length === 0) {
            chemicalsOverviewGrid.innerHTML = '';
            chemicalsList.forEach(chem => {
                const card = document.createElement('div');
                card.className = `dash-chem-card ${chem.active ? 'active-in-cabinet' : ''}`;
                card.dataset.chemId = chem.id;

                const cleanImg = (chem.imageUrl && chem.imageUrl.trim().length > 0)
                    ? chem.imageUrl.trim()
                    : 'https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=400';

                const imgThumb = `<div class="chem-card-thumb"><img src="${cleanImg}" alt="${chem.name}" onerror="this.onerror=null; this.src='https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=400';"></div>`;

                card.innerHTML = `
                    <div>
                        ${imgThumb}
                        <div class="dash-chem-top">
                            <div>
                                <h3 class="dash-chem-title">${chem.name}</h3>
                                <span class="dash-chem-formula">${chem.formula || 'N/A'}</span>
                            </div>
                            <div class="chem-active-container"></div>
                        </div>

                        <div style="margin: 0.4rem 0;">
                            <span class="badge badge-hazard">${chem.hazardCategory || 'General Storage'}</span>
                        </div>

                        <div class="dash-chem-stats">
                            <div>
                                <div class="stat-box-label">Max Temp</div>
                                <div class="stat-box-val" style="color: #e11d48;">${chem.maxTemperature} °C</div>
                            </div>
                            <div>
                                <div class="stat-box-label">Max Humidity</div>
                                <div class="stat-box-val" style="color: #0284c7;">${chem.maxHumidity} %</div>
                            </div>
                        </div>

                        <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.4rem 0.2rem; font-size: 0.85rem; color: #1e293b; font-weight: 500;">
                            <span>Current Status:</span>
                            <div class="chem-status-badge-container"></div>
                        </div>
                    </div>

                    <button class="btn-view-chem" onclick="openChemicalDetail(${chem.id})">
                        <i class="fa-solid fa-gauge-high"></i> View Dashboard
                    </button>
                `;
                chemicalsOverviewGrid.appendChild(card);
            });
        }

        // In-place value updates for zero image reload/flicker
        const allReadings = Object.values(latestMap);
        chemicalsList.forEach(chem => {
            const card = chemicalsOverviewGrid.querySelector(`[data-chem-id="${chem.id}"]`);
            if (!card) return;

            // Update active styling
            if (chem.active) {
                card.classList.add('active-in-cabinet');
            } else {
                card.classList.remove('active-in-cabinet');
            }

            // Update active pill
            const activeCont = card.querySelector('.chem-active-container');
            if (activeCont) {
                const activeHtml = chem.active
                    ? `<span class="badge-active-pill"><i class="fa-solid fa-circle-dot"></i> Monitored in Cabinet</span>`
                    : `<button onclick="event.stopPropagation(); setActiveFromGrid(${chem.id})" style="border: 1px solid #333333; background: #f8fafc; color: #333333; font-size: 0.72rem; font-weight: 600; padding: 4px 10px; border-radius: 9999px; cursor: pointer; display: inline-flex; align-items: center; gap: 4px;" title="Set as monitored chemical in cabinet"><i class="fa-solid fa-box-archive"></i> Set Active</button>`;
                if (activeCont.innerHTML !== activeHtml) {
                    activeCont.innerHTML = activeHtml;
                }
            }

            // Update status badge
            const chemReading = latestMap[chem.id] || (allReadings.length > 0 ? allReadings[0] : null);
            const statusCont = card.querySelector('.chem-status-badge-container');
            if (statusCont) {
                let statusBadge = '<span class="badge badge-neutral">Standby</span>';
                if (chemReading && (chemReading.temperature != null || chemReading.distance != null)) {
                    const isTempDanger = chem.maxTemperature != null && chemReading.temperature != null && chemReading.temperature >= chem.maxTemperature;
                    const isHumDanger = chem.maxHumidity != null && chemReading.humidity != null && chemReading.humidity > chem.maxHumidity;
                    const isEnvDanger = isTempDanger || isHumDanger;
                    const isInterruption = Boolean(chemReading.interruption) || (chemReading.status && chemReading.status.toLowerCase().includes('interruption'));

                    if (isEnvDanger && isInterruption) {
                        statusBadge = '<span class="badge badge-both-danger"><i class="fa-solid fa-triangle-exclamation"></i> DUAL DANGER</span>';
                    } else if (isInterruption) {
                        statusBadge = '<span class="badge badge-interruption-danger"><i class="fa-solid fa-shield-virus"></i> INTERRUPTION</span>';
                    } else if (isEnvDanger) {
                        statusBadge = '<span class="badge badge-danger"><i class="fa-solid fa-triangle-exclamation"></i> DANGER</span>';
                    } else {
                        statusBadge = '<span class="badge badge-safe"><i class="fa-solid fa-circle-check"></i> SAFE</span>';
                    }
                }
                if (statusCont.innerHTML !== statusBadge) {
                    statusCont.innerHTML = statusBadge;
                }
            }
        });

        applyDashboardSearchFilter();

    } catch (err) {
        console.error('Error loading chemical overview:', err);
    }
}

// ── Search Dashboard Overview Filter ────────────────────────────
function applyDashboardSearchFilter() {
    if (!chemicalsOverviewGrid) return;
    const cards = chemicalsOverviewGrid.querySelectorAll('.dash-chem-card');
    let visibleCount = 0;
    const query = (dashboardSearchQuery || '').toLowerCase().trim();

    cards.forEach(card => {
        const chemId = parseInt(card.dataset.chemId);
        const chem = (chemicalsList || []).find(c => Number(c.id) === Number(chemId));

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

    let noResultsMsg = document.getElementById('dash-no-search-results');
    if (cards.length > 0 && visibleCount === 0 && query) {
        if (!noResultsMsg) {
            noResultsMsg = document.createElement('p');
            noResultsMsg.id = 'dash-no-search-results';
            noResultsMsg.style.cssText = 'color: #1e293b; font-weight: 500; padding: 1.5rem; grid-column: 1 / -1; margin: 0;';
            chemicalsOverviewGrid.appendChild(noResultsMsg);
        }
        noResultsMsg.textContent = `No chemicals found matching "${dashboardSearchQuery}".`;
        noResultsMsg.style.display = 'block';
    } else if (noResultsMsg) {
        noResultsMsg.style.display = 'none';
    }
}

if (dashboardSearchInput) {
    dashboardSearchInput.addEventListener('input', (e) => {
        dashboardSearchQuery = e.target.value.toLowerCase().trim();
        applyDashboardSearchFilter();
    });
}

// ── Quick Set Active Chemical from Overview Grid ─────────────────
window.setActiveFromGrid = async function(chemId) {
    try {
        const res = await fetch(`/api/chemicals/active/${chemId}`, { method: 'POST' });
        if (res.ok) {
            await loadOverview();
        } else {
            alert('Failed to set monitored chemical in cabinet.');
        }
    } catch (err) {
        console.error('Error setting active chemical:', err);
    }
};

// ── VIEW 2: Open Single Chemical Dashboard ───────────────────────
window.openChemicalDetail = function(chemId) {
    const chem = chemicalsList.find(c => c.id === chemId);
    if (!chem) return;

    selectedChemical = chem;

    // Toggle views
    overviewView.style.display = 'none';
    singleChemicalView.style.display = 'block';

    // Update banner details & chemical image at the top
    const singleChemImg = document.getElementById('single-chem-img');
    const singleChemInstructions = document.getElementById('single-chem-instructions');

    if (singleChemImg) {
        const cleanSingleImg = (chem.imageUrl && chem.imageUrl.trim().length > 0)
            ? chem.imageUrl.trim()
            : 'https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=400';
        singleChemImg.onerror = function() {
            this.onerror = null;
            this.src = 'https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=400';
        };
        if (singleChemImg.src !== cleanSingleImg) {
            singleChemImg.src = cleanSingleImg;
        }
    }
    if (singleChemInstructions) {
        singleChemInstructions.textContent = chem.storageInstructions || 'Standard controlled storage guidelines apply.';
    }

    singleChemName.textContent = chem.name;
    singleChemFormula.textContent = chem.formula || 'N/A';
    singleChemHazard.textContent = chem.hazardCategory || 'General Storage';
    
    if (pageTitle) pageTitle.textContent = `${chem.name} Safety Dashboard`;
    if (alertChemTitle) alertChemTitle.textContent = chem.name;
    if (historyModalChemTitle) historyModalChemTitle.textContent = chem.name;

    // Update threshold hints on cards
    tempHintEl.textContent = `Max ${chem.maxTemperature} °C`;
    humHintEl.textContent = `Max ${chem.maxHumidity} %`;

    // Active in cabinet badge & button
    updateActiveCabinetUI(chem.active);

    // Update URL hash
    window.location.hash = `chem-${chem.id}`;

    // Reset card values before live fetch
    updateMetric(tempValEl, tempStatusEl, tempCardEl, null, '°C', false);
    updateMetric(humValEl, humStatusEl, humCardEl, null, '%', false);
    if (usValEl) usValEl.textContent = '-- cm';
    if (usCardEl) usCardEl.className = 'card';
    if (usStatusEl) {
        usStatusEl.className = (currentCabinetLock === 'LOCKED') ? 'badge badge-armed' : 'badge badge-disarmed';
        usStatusEl.innerHTML = (currentCabinetLock === 'LOCKED') ? '<i class="fa-solid fa-shield-halved"></i> ARMED' : '<i class="fa-solid fa-lock-open"></i> DISARMED';
    }
    lastUpdatedEl.innerHTML = `<i class="fa-regular fa-clock"></i> Connecting to ${chem.name} telemetry...`;

    // Immediate fetch
    fetchLatestReading();
    fetchAlerts();
    fetchDeviceControls();

    // Start fast polling
    if (pollTimerReadings) clearInterval(pollTimerReadings);
    if (pollTimerAlerts) clearInterval(pollTimerAlerts);
    if (pollTimerOverview) clearInterval(pollTimerOverview);
    if (pollTimerControls) clearInterval(pollTimerControls);

    pollTimerReadings = setInterval(fetchLatestReading, 800);
    pollTimerAlerts = setInterval(fetchAlerts, 3000);
    pollTimerControls = setInterval(fetchDeviceControls, 2000);
};

function updateActiveCabinetUI(isActive) {
    if (isActive) {
        singleChemActiveBadge.style.display = 'inline-flex';
        btnSetActiveCabinet.style.display = 'none';
    } else {
        singleChemActiveBadge.style.display = 'none';
        btnSetActiveCabinet.style.display = 'inline-block';
    }
}

// Set this chemical as the active chemical in the cabinet
if (btnSetActiveCabinet) {
    btnSetActiveCabinet.addEventListener('click', async () => {
        if (!selectedChemical) return;
        try {
            const res = await fetch(`/api/chemicals/active/${selectedChemical.id}`, { method: 'POST' });
            if (res.ok) {
                selectedChemical.active = true;
                updateActiveCabinetUI(true);
                // Also update local list
                chemicalsList.forEach(c => c.active = (c.id === selectedChemical.id));
                fetchLatestReading();
            }
        } catch (e) {
            console.error(e);
        }
    });
}

// Back to Overview Button
if (btnBackOverview) {
    btnBackOverview.addEventListener('click', () => {
        showOverview();
    });
}

function showOverview() {
    selectedChemical = null;
    if (pollTimerReadings) clearInterval(pollTimerReadings);
    if (pollTimerAlerts) clearInterval(pollTimerAlerts);
    if (pollTimerControls) clearInterval(pollTimerControls);

    singleChemicalView.style.display = 'none';
    overviewView.style.display = 'block';
    if (pageTitle) pageTitle.textContent = 'DashBoard';

    window.location.hash = '';
    loadOverview();

    // Keep overview updated with live sensor readings every 2 seconds
    if (pollTimerOverview) clearInterval(pollTimerOverview);
    pollTimerOverview = setInterval(loadOverview, 2000);
}

// ── Hardware Actuator Controls (Fan, Buzzer, Light) ──────────────
let isUserDraggingSlider = false;
let isUserDraggingBuzzerVolume = false;

async function fetchDeviceControls() {
    try {
        const res = await fetch('/api/controls');
        if (!res.ok) return;
        const data = await res.json();
        updateDeviceControlUI('fan', data.fan || 'NORMAL');
        updateDeviceControlUI('buzzer', data.buzzer || 'NORMAL');
        updateDeviceControlUI('light', data.light || 'NORMAL');
        if (data.lightBrightness !== undefined) {
            updateLightBrightnessUI(data.lightBrightness);
        }
        if (data.buzzerPattern) {
            updateBuzzerPatternUI(data.buzzerPattern);
        }
        if (data.buzzerVolume !== undefined) {
            updateBuzzerVolumeUI(data.buzzerVolume);
        }
        if (data.cabinetLock) {
            updateCabinetLockUI(data.cabinetLock, data.distanceThreshold);
        }
    } catch (e) {
        console.error('Error fetching hardware controls:', e);
    }
}
window.fetchDeviceControls = fetchDeviceControls;

// ── Cabinet Security Lock & Ultrasonic Threshold Controls ───────
function updateCabinetLockUI(state, threshold) {
    if (state) currentCabinetLock = state.toUpperCase();
    if (threshold !== undefined && threshold !== null) currentDistanceThreshold = Number(threshold);

    const isLocked = (currentCabinetLock === 'LOCKED');

    if (lockCardEl) {
        lockCardEl.classList.toggle('locked', isLocked);
        lockCardEl.classList.toggle('unlocked', !isLocked);
    }
    if (lockIcon) {
        lockIcon.className = `actuator-icon lock-icon ${isLocked ? 'locked' : 'unlocked'}`;
        lockIcon.innerHTML = isLocked ? '<i class="fa-solid fa-shield-halved"></i>' : '<i class="fa-solid fa-lock-open"></i>';
    }
    if (lockStatusPill) {
        lockStatusPill.className = `actuator-status-pill ${isLocked ? 'normal' : 'stopped'}`;
        lockStatusPill.innerHTML = isLocked
            ? '<i class="fa-solid fa-lock"></i> Locked & Armed'
            : '<i class="fa-solid fa-lock-open"></i> Unlocked';
    }
    if (lockDesc) {
        lockDesc.textContent = isLocked
            ? `Armed: Ultrasonic sensor wakes up. Any obstacle closer than ${currentDistanceThreshold.toFixed(1)} cm triggers Interruption Danger.`
            : 'Unlocked: Ultrasonic sensor disarmed. Obstacles/movement will not trigger alarms.';
    }
    if (btnLockArm) btnLockArm.classList.toggle('active', isLocked);
    if (btnLockDisarm) btnLockDisarm.classList.toggle('active', !isLocked);

    if (distanceThresholdVal) distanceThresholdVal.textContent = `${currentDistanceThreshold.toFixed(1)} cm`;
    if (distanceThresholdInput && document.activeElement !== distanceThresholdInput) {
        distanceThresholdInput.value = currentDistanceThreshold;
    }
    if (usHintEl) {
        usHintEl.textContent = isLocked ? `Wake limit: \u2264 ${currentDistanceThreshold.toFixed(1)} cm` : 'Sensor Disarmed';
    }
}

window.setCabinetLock = async function(state) {
    updateCabinetLockUI(state);
    try {
        const res = await fetch(`/api/controls/lock?state=${state}`, { method: 'POST' });
        if (res.status === 403) {
            alert('Permission denied. Only ADMIN or OPERATOR can change cabinet lock state.');
            fetchDeviceControls();
            return;
        }
        if (res.ok) {
            const data = await res.json();
            updateCabinetLockUI(data.cabinetLock, data.distanceThreshold);
            fetchLatestReading();
        }
    } catch (e) {
        console.error('Error setting cabinet lock:', e);
        fetchDeviceControls();
    }
};

window.sendDistanceThreshold = async function(val) {
    const num = parseFloat(val);
    if (isNaN(num) || num <= 0) return;
    try {
        const res = await fetch(`/api/controls/lock/threshold?threshold=${num}`, { method: 'POST' });
        if (res.status === 403) {
            alert('Permission denied. Only ADMIN or OPERATOR can change distance threshold.');
            fetchDeviceControls();
            return;
        }
        if (res.ok) {
            const data = await res.json();
            updateCabinetLockUI(data.cabinetLock, data.distanceThreshold);
            fetchLatestReading();
        }
    } catch (e) {
        console.error('Error updating distance threshold:', e);
    }
};

function updateLightBrightnessUI(val) {
    const slider = document.getElementById('light-brightness-slider');
    const valBadge = document.getElementById('light-brightness-val');
    if (valBadge) valBadge.textContent = `${val}%`;
    if (slider && !isUserDraggingSlider) {
        slider.value = val;
    }
}

window.onLightBrightnessInput = function(val) {
    isUserDraggingSlider = true;
    const valBadge = document.getElementById('light-brightness-val');
    if (valBadge) valBadge.textContent = `${val}%`;
};

window.sendLightBrightness = async function(val) {
    isUserDraggingSlider = false;
    try {
        const res = await fetch(`/api/controls/light/brightness?value=${val}`, {
            method: 'POST'
        });
        if (res.status === 403) {
            alert('Permission denied. Only ADMIN or OPERATOR can adjust light brightness.');
            fetchDeviceControls();
            return;
        }
        if (res.ok) {
            const data = await res.json();
            if (data.lightBrightness !== undefined) {
                updateLightBrightnessUI(data.lightBrightness);
            }
        }
    } catch (e) {
        console.error('Error updating brightness:', e);
    }
};

// Buzzer Pattern, Volume, and Tone UI Handlers
function updateBuzzerPatternUI(pattern) {
    const group = document.getElementById('buzzer-pattern-group');
    const valBadge = document.getElementById('buzzer-pattern-val');
    if (valBadge) {
        const mapName = {
            'CONTINUOUS': 'Continuous',
            'URGENT': 'Fast Beep',
            'PULSE': 'Slow Pulse',
            'CHIRP': 'Double Chirp'
        };
        valBadge.textContent = mapName[pattern] || pattern;
    }
    if (group) {
        const buttons = group.querySelectorAll('.btn-pattern-pill');
        buttons.forEach(btn => {
            if (btn.dataset.pattern === pattern) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    }
}

window.setBuzzerPattern = async function(pattern) {
    updateBuzzerPatternUI(pattern);
    try {
        const res = await fetch(`/api/controls/buzzer/pattern?pattern=${pattern}`, {
            method: 'POST'
        });
        if (res.status === 403) {
            alert('Permission denied. Only ADMIN or OPERATOR can change buzzer pattern.');
            fetchDeviceControls();
            return;
        }
        if (res.ok) {
            const data = await res.json();
            if (data.buzzerPattern) updateBuzzerPatternUI(data.buzzerPattern);
        }
    } catch (e) {
        console.error('Error updating buzzer pattern:', e);
    }
};

function updateBuzzerVolumeUI(val) {
    const slider = document.getElementById('buzzer-volume-slider');
    const valBadge = document.getElementById('buzzer-volume-val');
    if (valBadge) valBadge.textContent = `${val}%`;
    if (slider && !isUserDraggingBuzzerVolume) {
        slider.value = val;
    }
}

window.onBuzzerVolumeInput = function(val) {
    isUserDraggingBuzzerVolume = true;
    const valBadge = document.getElementById('buzzer-volume-val');
    if (valBadge) valBadge.textContent = `${val}%`;
};

window.sendBuzzerVolume = async function(val) {
    isUserDraggingBuzzerVolume = false;
    try {
        const res = await fetch(`/api/controls/buzzer/volume?value=${val}`, {
            method: 'POST'
        });
        if (res.status === 403) {
            alert('Permission denied. Only ADMIN or OPERATOR can adjust buzzer volume.');
            fetchDeviceControls();
            return;
        }
        if (res.ok) {
            const data = await res.json();
            if (data.buzzerVolume !== undefined) updateBuzzerVolumeUI(data.buzzerVolume);
        }
    } catch (e) {
        console.error('Error updating buzzer volume:', e);
    }
};

function updateDeviceControlUI(device, mode) {
    currentDeviceModes[device] = mode;
    const card = document.getElementById(`actuator-${device}-card`);
    const pill = document.getElementById(`${device}-status-pill`);
    const desc = document.getElementById(`${device}-desc`);
    const btnStop = document.getElementById(`btn-${device}-stop`);
    const btnResume = document.getElementById(`btn-${device}-resume`);

    if (!card || !pill || !btnStop || !btnResume) return;

    if (mode === 'STOPPED') {
        card.classList.remove('normal');
        card.classList.add('stopped');
        pill.className = 'actuator-status-pill stopped';
        pill.innerHTML = '<i class="fa-solid fa-ban"></i> Stopped';
        
        let deviceName = device === 'fan' ? 'Exhaust fan' : (device === 'buzzer' ? 'Alarm buzzer' : 'Cabinet light');
        if (desc) desc.textContent = `Stopped: ${deviceName} is turned off until Resume is clicked.`;

        btnStop.classList.add('active');
        btnResume.classList.remove('active');

        if (device === 'light') {
            const slider = document.getElementById('light-brightness-slider');
            const dimmerCont = document.getElementById('light-dimmer-container');
            if (slider) slider.disabled = true;
            if (dimmerCont) dimmerCont.style.opacity = '0.5';
        }
        if (device === 'buzzer') {
            const volSlider = document.getElementById('buzzer-volume-slider');
            const dimmerCont = document.getElementById('buzzer-dimmer-container');
            if (volSlider) volSlider.disabled = true;
            if (dimmerCont) {
                dimmerCont.style.opacity = '0.5';
                dimmerCont.querySelectorAll('.btn-pattern-pill').forEach(b => b.disabled = true);
            }
        }
    } else {
        card.classList.remove('stopped');
        card.classList.add('normal');
        pill.className = 'actuator-status-pill normal';
        pill.innerHTML = '<i class="fa-solid fa-circle-check"></i> Normal';

        if (desc) {
            if (device === 'fan') desc.textContent = 'Automatic: Operates normally based on hazard limits.';
            else if (device === 'buzzer') desc.textContent = 'Automatic: Sounds alert siren when limits breached.';
            else if (device === 'light') desc.textContent = 'Automatic: Illuminates during hazard at selected PWM brightness.';
        }

        btnStop.classList.remove('active');
        btnResume.classList.add('active');

        if (device === 'light') {
            const slider = document.getElementById('light-brightness-slider');
            const dimmerCont = document.getElementById('light-dimmer-container');
            if (slider) slider.disabled = false;
            if (dimmerCont) dimmerCont.style.opacity = '1';
        }
        if (device === 'buzzer') {
            const volSlider = document.getElementById('buzzer-volume-slider');
            const dimmerCont = document.getElementById('buzzer-dimmer-container');
            if (volSlider) volSlider.disabled = false;
            if (dimmerCont) {
                dimmerCont.style.opacity = '1';
                dimmerCont.querySelectorAll('.btn-pattern-pill').forEach(b => b.disabled = false);
            }
        }
    }
}

window.setDeviceMode = async function(device, mode) {
    try {
        // Optimistic UI update
        updateDeviceControlUI(device, mode);

        const res = await fetch(`/api/controls/${device}?mode=${mode}`, {
            method: 'POST'
        });

        if (res.status === 403) {
            alert('Permission denied. Only ADMIN or OPERATOR can change hardware controls.');
            fetchDeviceControls();
            return;
        }

        if (!res.ok) {
            alert('Failed to update actuator mode.');
            fetchDeviceControls();
            return;
        }

        const data = await res.json();
        if (data.status === 'OK' && data.mode) {
            updateDeviceControlUI(device, data.mode);
        }
    } catch (err) {
        console.error('Error changing actuator mode:', err);
        fetchDeviceControls();
    }
};

// Helper for case-insensitive string comparison
String.prototype.equalsIgnoreCase = function(str) {
    return this.toLowerCase() === (str || '').toLowerCase();
};

// ── Fetch Latest Reading for Single Chemical ─────────────────────
async function fetchLatestReading() {
window.fetchLatestReading = fetchLatestReading;
    if (!selectedChemical) return;

    try {
        let data = null;
        const url = `/api/readings/latest?chemicalId=${selectedChemical.id}`;
        const response = await fetch(url);
        if (response.ok && response.status !== 204) {
            data = await response.json();
        }

        // If specific chemical has no reading yet, fallback to generic latest reading
        if (!data || data.temperature == null) {
            const fallback = await fetch('/api/readings/latest');
            if (fallback.ok && fallback.status !== 204) {
                data = await fallback.json();
            }
        }

        if (!data || data.temperature == null) return;

        const tempLimit = selectedChemical.maxTemperature;
        const humLimit = selectedChemical.maxHumidity;

        // 1. Environmental Danger Evaluation
        const isTempDanger = (data.temperature !== null && data.temperature !== undefined)
            ? (data.temperature >= tempLimit)
            : false;

        const isHumDanger = (data.humidity !== null && data.humidity !== undefined)
            ? (data.humidity > humLimit)
            : false;

        const isEnvDanger = isTempDanger || isHumDanger;

        updateMetric(tempValEl, tempStatusEl, tempCardEl, data.temperature, '°C', isTempDanger);
        updateMetric(humValEl, humStatusEl, humCardEl, data.humidity, '%', isHumDanger);

        // 2. Ultrasonic Radar & Interruption Danger Evaluation
        const isCabinetLocked = (currentCabinetLock === 'LOCKED');
        const distLimit = (data.distanceThreshold != null) ? data.distanceThreshold : currentDistanceThreshold;
        if (data.distanceThreshold != null) {
            currentDistanceThreshold = data.distanceThreshold;
        }

        const isInterruptionDanger = isCabinetLocked && (
            Boolean(data.interruption) ||
            (data.status && data.status.toLowerCase().includes('interruption')) ||
            (data.distance != null && data.distance > 0 && data.distance <= distLimit)
        );

        if (usValEl) {
            if (data.distance != null && !isNaN(data.distance)) {
                usValEl.textContent = `${Number(data.distance).toFixed(1)} cm`;
            } else {
                usValEl.textContent = '-- cm';
            }
        }

        if (usCardEl && usStatusEl) {
            if (!isCabinetLocked) {
                // Cabinet unlocked / disarmed
                usCardEl.className = 'card';
                usStatusEl.className = 'badge badge-disarmed';
                usStatusEl.innerHTML = '<i class="fa-solid fa-lock-open"></i> DISARMED';
                if (usHintEl) usHintEl.textContent = 'Lock cabinet to arm';
                if (usValEl) usValEl.classList.remove('danger-text');
            } else if (isInterruptionDanger) {
                // Interruption danger state!
                usCardEl.className = 'card card-danger card-interruption-danger';
                usStatusEl.className = 'badge badge-interruption-danger';
                usStatusEl.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> BREACH';
                if (usHintEl) usHintEl.textContent = `Breached \u2264 ${distLimit.toFixed(1)} cm`;
                if (usValEl) usValEl.classList.add('danger-text');
            } else {
                // Armed and secure
                usCardEl.className = 'card card-safe';
                usStatusEl.className = 'badge badge-armed';
                usStatusEl.innerHTML = '<i class="fa-solid fa-shield-halved"></i> ARMED';
                if (usHintEl) usHintEl.textContent = `Wake limit: \u2264 ${distLimit.toFixed(1)} cm`;
                if (usValEl) usValEl.classList.remove('danger-text');
            }
        }

        // 3. Multi-Hazard Status Meta Bar Banner
        if (lastUpdatedEl) {
            const date = data.createdAt ? new Date(data.createdAt) : new Date();
            const timeString = date.toLocaleTimeString();

            let statusLabel = '';
            if (isEnvDanger && isInterruptionDanger) {
                statusLabel = '<span class="badge badge-both-danger" style="margin-left: 6px;"><i class="fa-solid fa-triangle-exclamation"></i> DUAL DANGER (ENV + INTERRUPTION)</span>';
            } else if (isInterruptionDanger) {
                statusLabel = '<span class="badge badge-interruption-danger" style="margin-left: 6px;"><i class="fa-solid fa-shield-virus"></i> INTERRUPTION DANGER</span>';
            } else if (isEnvDanger) {
                statusLabel = '<span class="badge badge-danger" style="margin-left: 6px;"><i class="fa-solid fa-triangle-exclamation"></i> ENVIRONMENTAL DANGER</span>';
            } else {
                statusLabel = '<span class="badge badge-safe" style="margin-left: 6px;"><i class="fa-solid fa-circle-check"></i> SAFE (SECURED)</span>';
            }

            const cabinetNote = selectedChemical.active
                ? '<span style="color:#22c55e; font-weight: 600; margin-left: 6px;">[Cabinet Monitored]</span>'
                : '<span style="color:#0284c7; font-weight: 500; margin-left: 6px;">[Ambient Sensor Telemetry]</span>';

            lastUpdatedEl.innerHTML = `<i class="fa-regular fa-clock"></i> Latest Reading: <strong>${timeString}</strong> &bull; Chemical: <strong>${selectedChemical.name}</strong> &bull; Status: ${statusLabel} ${cabinetNote}`;
        }

    } catch (error) {
        console.error('Error fetching reading:', error);
    }
}

// ── Fetch Alerts for Single Chemical ─────────────────────────────
async function fetchAlerts() {
    if (!selectedChemical) return;

    try {
        const response = await fetch(`/api/alerts?chemicalId=${selectedChemical.id}`);
        if (!response.ok) return;

        const alerts = await response.json();
        alertsTbody.innerHTML = '';

        if (!alerts || alerts.length === 0) {
            alertsTbody.innerHTML = `<tr><td colspan="3" style="text-align: center; color: #1e293b; font-weight: 500;">No active alerts for ${selectedChemical.name}. Levels within safe thresholds.</td></tr>`;
            return;
        }

        alerts.forEach(alert => {
            const tr = document.createElement('tr');
            const date = new Date(alert.createdAt);
            const timeString = date.toLocaleTimeString() + ' - ' + date.toLocaleDateString();

            const st = (alert.status || '').toLowerCase();
            let badgeClass = 'badge-danger';
            let statusText = 'DANGER';

            if (st === 'interruption_danger' || alert.dangerType === 'INTERRUPTION') {
                badgeClass = 'badge-interruption-danger';
                statusText = '<i class="fa-solid fa-shield-virus"></i> INTERRUPTION';
            } else if (st === 'both_danger' || alert.dangerType === 'BOTH') {
                badgeClass = 'badge-both-danger';
                statusText = '<i class="fa-solid fa-triangle-exclamation"></i> DUAL DANGER';
            } else if (st === 'danger' || alert.dangerType === 'ENVIRONMENTAL') {
                badgeClass = 'badge-danger';
                statusText = '<i class="fa-solid fa-temperature-arrow-up"></i> ENV DANGER';
            } else {
                badgeClass = 'badge-safe';
                statusText = 'SAFE';
            }

            const reason = alert.alertReason || `Breached ${selectedChemical.name} safe limits`;

            tr.innerHTML = `
                <td>${timeString}</td>
                <td><span class="badge ${badgeClass}">${statusText}</span></td>
                <td><small style="color: #991b1b; font-weight: 600;">${reason}</small></td>
            `;
            alertsTbody.appendChild(tr);
        });

    } catch (error) {
        console.error('Error fetching alerts:', error);
    }
}

// ── Full History Modal ───────────────────────────────────────────
async function fetchFullHistory() {
    if (!selectedChemical) return;

    try {
        const response = await fetch(`/api/history?chemicalId=${selectedChemical.id}`);
        if (!response.ok) return;

        const history = await response.json();
        historyTbody.innerHTML = '';

        if (!history || history.length === 0) {
            historyTbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: #1e293b; font-weight: 500;">No readings logged for ${selectedChemical.name}.</td></tr>`;
            return;
        }

        history.forEach(row => {
            const tr = document.createElement('tr');
            const date = new Date(row.createdAt);
            const timeString = date.toLocaleTimeString() + ' - ' + date.toLocaleDateString();

            const st = (row.status || '').toLowerCase();
            let badgeClass = 'badge-danger';
            let statusText = 'DANGER';

            if (st === 'interruption_danger' || row.dangerType === 'INTERRUPTION') {
                badgeClass = 'badge-interruption-danger';
                statusText = 'INTERRUPTION';
            } else if (st === 'both_danger' || row.dangerType === 'BOTH') {
                badgeClass = 'badge-both-danger';
                statusText = 'DUAL DANGER';
            } else if (st === 'danger' || row.dangerType === 'ENVIRONMENTAL') {
                badgeClass = 'badge-danger';
                statusText = 'ENV DANGER';
            } else {
                badgeClass = 'badge-safe';
                statusText = 'SAFE';
            }

            const distStr = (row.distance != null) ? `${Number(row.distance).toFixed(1)} cm` : '--';

            tr.innerHTML = `
                <td>${timeString}</td>
                <td>${row.temperature != null ? row.temperature.toFixed(1) + ' °C' : '--'}</td>
                <td>${row.humidity != null ? row.humidity.toFixed(1) + ' %' : '--'}</td>
                <td>${distStr}</td>
                <td><span class="badge ${badgeClass}">${statusText}</span></td>
            `;
            historyTbody.appendChild(tr);
        });
    } catch (error) {
        console.error('Error fetching full history:', error);
    }
}

if (historyBtn) {
    historyBtn.onclick = () => {
        fetchFullHistory();
        historyModal.style.display = "block";
    };
}

if (closeBtn) {
    closeBtn.onclick = () => {
        historyModal.style.display = "none";
    };
}

window.onclick = (event) => {
    if (event.target === historyModal) {
        historyModal.style.display = "none";
    }
};

// ── Sidebar Toggle Logic (Referenced from downloads/project) ────
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

// ── Initial Boot & Hash Routing ─────────────────────────────────
async function initApp() {
    await loadOverview();

    // Check if URL specifies a chemical (e.g. index.html#chem-1 or ?chemicalId=1)
    const urlParams = new URLSearchParams(window.location.search);
    const paramChemId = urlParams.get('chemicalId');
    const hash = window.location.hash;

    if (paramChemId) {
        openChemicalDetail(parseInt(paramChemId));
    } else if (hash && hash.startsWith('#chem-')) {
        const id = parseInt(hash.replace('#chem-', ''));
        if (id) openChemicalDetail(id);
    } else {
        if (pollTimerOverview) clearInterval(pollTimerOverview);
        pollTimerOverview = setInterval(loadOverview, 2000);
    }
}

initApp();
