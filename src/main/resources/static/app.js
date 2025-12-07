// javascript
// 파일: `src/main/resources/static/app.js`

const API_BASE = '/api';

/**
 * 공통 Fetch Wrapper (Session Cookie 포함)
 */
async function api(endpoint, method = 'GET', body = null) {
    const options = {
        method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        cache: 'no-cache'
    };
    if (body) options.body = JSON.stringify(body);

    const res = await fetch(API_BASE + endpoint, options);
    if (!res.ok) {
        const err = await res.json().catch(() => ({ message: res.statusText }));
        throw new Error(err.message || `Error ${res.status}`);
    }
    if (res.status === 204) return null;
    return res.json().catch(() => null);
}

// === 전역 상태 ===
let currentUser = null;

// === Writer View 상태 관리 ===
let writerState = {
    charId: null,
    charName: '',
    routeId: null,
    routeTitle: '',
    epId: null,
    epText: '',
    routes: [],
    episodes: []
};

let editingEpId = null;

/* ---------------------------
   유틸: 안전한 HTML 이스케이프 (XSS 방지)
   --------------------------- */
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str).replace(/[&<>"']/g, function (m) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m];
    });
}

// ==========================================
// 1. 인증 로직
// ==========================================

function showAuthTab(tab) {
    document.getElementById('login-tab').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('signup-tab').style.display = tab === 'signup' ? 'block' : 'none';
    const buttons = document.querySelectorAll('.tab-btn');
    buttons.forEach(b => b.classList.remove('active'));
    if (tab === 'login') buttons[0].classList.add('active');
    else buttons[1].classList.add('active');
}

async function checkLogin() {
    try {
        const user = await api('/auth/me');
        if (user) handleLoginSuccess(user);
    } catch (e) {
        console.log('Not logged in');
        const authSection = document.getElementById('auth-section');
        if (authSection) authSection.style.display = 'block';
    }
}

document.getElementById('login-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        const res = await api('/auth/login', 'POST', { email, password });
        handleLoginSuccess({ name: res.name, role: res.role });
    } catch (err) { alert('로그인 실패: ' + err.message); }
});

document.getElementById('signup-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
        const req = {
            email: document.getElementById('signup-email').value,
            password: document.getElementById('signup-password').value,
            name: document.getElementById('signup-name').value,
            role: document.getElementById('signup-role').value
        };
        await api('/auth/signup', 'POST', req);
        alert('가입 신청 완료! 로그인해주세요.');
        showAuthTab('login');
    } catch (err) { alert('가입 실패: ' + err.message); }
});

document.getElementById('logout-btn')?.addEventListener('click', async () => {
    await api('/auth/logout', 'POST');
    location.reload();
});

function handleLoginSuccess(user) {
    currentUser = user;
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('dashboard-section').style.display = 'block';
    document.getElementById('user-info').style.display = 'block';
    document.getElementById('welcome-msg').innerText = `${user.name} (${user.role})`;

    const role = user.role;
    if (['SCENARIO_WRITER', 'GAME_ADMIN'].includes(role)) document.getElementById('nav-writer').style.display = 'inline-block';
    if (['BALANCE_TUNER', 'GAME_ADMIN'].includes(role)) document.getElementById('nav-admin').style.display = 'inline-block';

    switchView('player-view');
    loadCharacters();
}

function switchView(viewId) {
    document.querySelectorAll('.view-panel').forEach(el => el.style.display = 'none');
    document.getElementById(viewId).style.display = 'block';

    if (viewId === 'admin-view') {
        // 권한 요청 관리는 GAME_ADMIN만 볼 수 있음
        const roleRequestPanel = document.getElementById('role-request-panel');
        if (roleRequestPanel) {
            if (currentUser && currentUser.role === 'GAME_ADMIN') {
                roleRequestPanel.style.display = 'block';
                loadRoleRequests();
            } else {
                roleRequestPanel.style.display = 'none';
            }
        }
        loadAnalyticsRoutes();
        loadAdminLogs();
    }
    if (viewId === 'writer-view') {
        resetWriterView('char');
    }
}

async function loadAdminLogs() {
    const tbody = document.querySelector('#admin-log-table tbody');
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">로딩 중...</td></tr>';
    try {
        const logs = await api('/admin/analytics/logs');
        tbody.innerHTML = '';
        if (!logs || logs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">기록된 로그가 없습니다.</td></tr>';
            return;
        }
        logs.forEach(log => {
            const tr = document.createElement('tr');
            const date = new Date(log.createdAt).toLocaleString();
            let badgeClass = '';
            if (log.action === 'CREATE') badgeClass = 'badge-create';
            else if (log.action === 'UPDATE') badgeClass = 'badge-update';
            else if (log.action === 'DELETE') badgeClass = 'badge-delete';
            tr.innerHTML = `
                <td>${date}</td>
                <td>${log.adminId}</td>
                <td class="${badgeClass}">${log.action}</td>
                <td>${log.targetType}</td>
                <td>${log.targetId}</td>
                <td>${escapeHtml(log.description)}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error(err);
        tbody.innerHTML = `<tr><td colspan="6" style="color:red; text-align:center;">로그 로드 실패: ${escapeHtml(err.message)}</td></tr>`;
    }
}

// ==========================================
// 2. 시나리오 작가 로직
// ==========================================

function resetWriterView(level) {
    ['panel-char', 'panel-route', 'panel-ep', 'panel-choice'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });

    const navRoute = document.getElementById('nav-route');
    const navEp = document.getElementById('nav-ep');
    const navChoice = document.getElementById('nav-choice');
    if (navRoute) navRoute.style.display = 'none';
    if (navEp) navEp.style.display = 'none';
    if (navChoice) navChoice.style.display = 'none';

    if (level === 'char') {
        document.getElementById('panel-char').style.display = 'block';
        loadWriterCharacters();
    } else if (level === 'route') {
        document.getElementById('panel-route').style.display = 'block';
        document.getElementById('nav-route').style.display = 'inline';
        document.getElementById('selected-char-name').innerText = ` - ${writerState.charName}`;
        loadWriterRoutes(writerState.charId);
    } else if (level === 'ep') {
        document.getElementById('panel-ep').style.display = 'block';
        document.getElementById('nav-route').style.display = 'inline';
        document.getElementById('nav-ep').style.display = 'inline';
        document.getElementById('selected-route-title').innerText = ` - ${writerState.routeTitle}`;
        loadWriterEpisodes(writerState.routeId);
    } else if (level === 'choice') {
        document.getElementById('panel-choice').style.display = 'block';
        document.getElementById('nav-route').style.display = 'inline';
        document.getElementById('nav-ep').style.display = 'inline';
        document.getElementById('nav-choice').style.display = 'inline';
        document.getElementById('selected-ep-id').innerText = writerState.epId;
        document.getElementById('preview-ep-text').innerText = writerState.epText;
        loadEpisodeOptionsForChoice();
        loadWriterChoices(writerState.epId);
    }
}

async function loadEpisodeOptionsForChoice() {
    const selPass = document.getElementById('sel-next-pass');
    const selFail = document.getElementById('sel-next-fail');
    if (!selPass || !selFail) return;
    selPass.innerHTML = '<option value="">성공 시 이동할 에피소드...</option>';
    selFail.innerHTML = '<option value="">실패 시 이동할 에피소드...</option>';
    try {
        const eps = await api(`/story/routes/${writerState.routeId}/episodes`);
        eps.forEach(ep => {
            const title = (ep.title && ep.title.trim()) ? ep.title.trim() : (ep.text || '').substring(0, 15);
            const label = `[EP.${ep.id}] ${escapeHtml(title)}${ep.isEnding ? ` (${escapeHtml(formatEndingLabel(ep.endingType))})` : ''}`;
            const opt1 = document.createElement('option');
            opt1.value = ep.id;
            opt1.innerText = label;
            selPass.appendChild(opt1);
            const opt2 = document.createElement('option');
            opt2.value = ep.id;
            opt2.innerText = label;
            selFail.appendChild(opt2);
        });
    } catch (e) { console.error(e); }
}

// --- 2-1. 캐릭터 관리 ---
async function loadWriterCharacters() {
    const list = document.getElementById('list-char');
    if (!list) return;
    list.innerHTML = '';
    try {
        const chars = await api('/game/characters');
        chars.forEach(c => {
            const li = document.createElement('li');
            li.innerHTML = `
                <div><strong>${escapeHtml(c.name)}</strong> <small>(${escapeHtml(c.gender)}, ${escapeHtml(c.personality)})</small></div>
                <div>
                    <span style="font-size:0.8em; color:#888; margin-right:10px; cursor:pointer;">ID: ${c.id} &gt;</span>
                    <button class="btn-sm" style="background:#dc3545; color:white;" onclick="deleteCharacter(event, ${c.id})">🗑️</button>
                </div>
            `;
            li.onclick = (e) => {
                if (e.target.tagName === 'BUTTON') return;
                writerState.charId = c.id;
                writerState.charName = c.name;
                resetWriterView('route');
            };
            list.appendChild(li);
        });
    } catch (e) { console.error(e); }
}

async function deleteCharacter(event, charId) {
    event.stopPropagation();
    if (!confirm("캐릭터를 삭제하시겠습니까?\n포함된 모든 루트와 에피소드가 함께 삭제됩니다!")) return;
    try {
        await api(`/story/characters/${charId}`, 'DELETE');
        alert("삭제되었습니다.");
        loadWriterCharacters();
    } catch (e) { alert(e.message); }
}

document.getElementById('form-create-char')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        name: fd.get('name'),
        gender: fd.get('gender'),
        personality: fd.get('personality'),
        affinityCap: parseInt(fd.get('affinityCap'))
    };
    try {
        await api('/story/characters', 'POST', data);
        e.target.reset();
        loadWriterCharacters();
    } catch (err) { alert(err.message); }
});

// --- 2-2. 루트 관리 ---
async function loadWriterRoutes(charId) {
    const list = document.getElementById('list-route');
    if (!list) return;
    list.innerHTML = '';
    try {
        const routes = await api(`/story/characters/${charId}/routes`);
        writerState.routes = routes;
        if (!routes || routes.length === 0) list.innerHTML = '<li style="cursor:default; background:#eee;">생성된 루트가 없습니다.</li>';
        routes.forEach(r => {
            const li = document.createElement('li');
            li.innerHTML = `
                <div><strong>${escapeHtml(r.title)}</strong></div>
                <div>
                    <span style="font-size:0.8em; color:#888; margin-right:10px; cursor:pointer;">ID: ${r.id} &gt;</span>
                    <button class="btn-sm" style="background:#dc3545; color:white;" onclick="deleteRoute(event, ${r.id})">🗑️</button>
                </div>
            `;
            li.onclick = (e) => {
                if (e.target.tagName === 'BUTTON') return;
                writerState.routeId = r.id;
                writerState.routeTitle = r.title;
                resetWriterView('ep');
            };
            list.appendChild(li);
        });
    } catch (e) { console.error(e); }
}

async function deleteRoute(event, routeId) {
    event.stopPropagation();
    if (!confirm("루트를 삭제하시겠습니까?\n포함된 모든 에피소드가 함께 삭제됩니다!")) return;
    try {
        await api(`/story/routes/${routeId}`, 'DELETE');
        alert("삭제되었습니다.");
        loadWriterRoutes(writerState.charId);
    } catch (e) { alert(e.message); }
}

document.getElementById('form-create-route')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        characterId: writerState.charId,
        title: fd.get('title'),
        minAffectionRequired: parseInt(fd.get('minAffection') || 0),
        trueEndingThreshold: parseInt(fd.get('trueThreshold') || 0),
        warningText: fd.get('warning'),
        badEndingEpId: 0,
        normalEndingEpId: 0,
        trueEndingEpId: 0
    };
    try {
        await api('/story/routes', 'POST', data);
        e.target.reset();
        loadWriterRoutes(writerState.charId);
    } catch (err) { alert(err.message); }
});

// --- 2-3. 에피소드 관리 ---
// 에피소드 목록 로드 및 렌더링 (제목 표시, XSS 방지)
// loadWriterEpisodes 함수 교체 (표시용 엔딩 라벨 변경)
// javascript
async function loadWriterEpisodes(routeId) {
    const container = document.getElementById('list-ep');
    if (!container) return;
    container.innerHTML = '';

    try {
        const eps = await api(`/story/routes/${routeId}/episodes`);
        writerState.episodes = eps || [];

        if (!eps || eps.length === 0) {
            container.innerHTML = '<p style="padding:10px; color:#999;">에피소드가 없습니다.</p>';
            return;
        }

        eps.forEach(ep => {
            const div = document.createElement('div');
            div.className = `ep-card ${ep.isEnding ? 'ending' : ''}`;

            // 가능한 제목 키들을 확인: title, epTitle, name 등
            let titleCandidate = (ep.title || ep.epTitle || ep.name || '').toString().trim();

            // 숫자(또는 "EP #13" 같은 형태)만 있는 경우 제목으로 보지 않음
            const onlyNumberPattern = /^\s*(?:EP[\s#:]*)?\d+\s*$/i;
            if (onlyNumberPattern.test(titleCandidate)) {
                titleCandidate = '';
            }

            // 본문에서 대체 제목 추출 (연속 공백 정리)
            const cleanedText = (ep.text || '').replace(/\s+/g, ' ').trim();
            const fallbackTitle = cleanedText ? (cleanedText.length > 40 ? cleanedText.substring(0, 40).trim() + '...' : cleanedText) : null;

            const displayTitle = titleCandidate || (fallbackTitle || `EP #${ep.id}`);
            const fullTooltip = titleCandidate || cleanedText || `EP #${ep.id}`;

            div.innerHTML = `
                <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:5px;">
                    <span class="ep-title" title="${escapeHtml(fullTooltip)}">${escapeHtml(displayTitle)}</span>
                    <div>
                        <button class="btn-sm" style="background:#00b894; color:white; margin-right:5px;" onclick="event.stopPropagation(); setStartEpisode(event, ${ep.id})">🚩시작점</button>
                        <button class="btn-sm" style="background:#ffc107; color:#333; border:none; padding:2px 8px; border-radius:4px; font-size:0.8em; cursor:pointer;" onclick="event.stopPropagation(); startEditEpisode(event, ${ep.id})">✏️수정</button>
                        <button class="btn-sm" style="background:#dc3545; color:white;" onclick="event.stopPropagation(); deleteEpisode(event, ${ep.id})">🗑️삭제</button>
                    </div>
                </div>
                <div class="ep-text" style="font-size:0.85em; color:#555;">
                    ${escapeHtml(ep.text)}
                </div>
                ${ep.isEnding ? `<div style="color:red; font-size:0.8em; margin-top:5px; font-weight:bold;">[${escapeHtml(formatEndingLabel(ep.endingType))}]</div>` : ''}
            `;

            // 카드 클릭 -> 선택지 관리로 이동
            div.onclick = () => {
                writerState.epId = ep.id;
                writerState.epText = ep.text;
                resetWriterView('choice');
            };

            container.appendChild(div);
        });
    } catch (e) {
        console.error('loadWriterEpisodes error', e);
        container.innerHTML = `<p style="color:red; padding:10px;">에피소드 로드 실패: ${escapeHtml(e.message || String(e))}</p>`;
    }
}

// javascript
// 파일: `src/main/resources/static/app.js` 에 추가/교체할 코드

// 안전한 게임 리셋 처리: 서버 호출 후 메인(캐릭터 선택)으로 복구
async function finishGame() {
    console.log('>>> [종료] finishGame start');
    try {
        await api('/game/reset', 'POST');
    } catch (err) {
        console.error('>>> [종료] reset API 실패', err);
        alert('게임 초기화 실패: ' + (err.message || String(err)));
        return;
    }

    try {
        console.log('>>> [종료] view 전환 및 캐릭터 로드 시도');

        // 뷰 전환(안전하게)
        try { switchView('player-view'); } catch (e) { console.debug('switchView 오류(무시):', e); }

        // 게임 관련 화면 숨기기
        ['game-screen', 'report-screen', 'route-select-screen'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'none';
        });

        // 캐릭터 선택 화면 노출 및 로딩 표시
        const charScreen = document.getElementById('char-selection-screen');
        const container = document.getElementById('char-list');
        if (charScreen) charScreen.style.display = 'block';
        if (container) {
            container.style.display = '';
            container.innerHTML = '<p style="padding:20px; font-weight:bold;">🔄 메인 로드 중...</p>';
        }

        console.log('>>> [종료] calling loadCharacters');
        await loadCharacters();

        console.log('>>> [종료] returned to main successfully');
    } catch (err) {
        console.error('>>> [종료] 메인 복구 실패', err);
        const container = document.getElementById('char-list');
        if (container) {
            container.innerHTML = `<p style="padding:20px; color:red;">메인 복구 실패: ${escapeHtml(err.message || String(err))}</p>`;
        } else {
            alert('메인 복구 실패: ' + (err.message || String(err)));
        }
    }
}

// DOMContentLoaded 초기화: 기존 블록과 병합하거나 교체
document.addEventListener('DOMContentLoaded', () => {
    try { checkLogin(); } catch (e) { console.error('checkLogin 오류', e); }

    // "처음부터 다시 하기" 버튼 안전 바인딩
    const resetBtn = document.getElementById('btn-reset-game');
    if (resetBtn) {
        if (!resetBtn.dataset.hasResetHandler) {
            resetBtn.addEventListener('click', async (e) => {
                e.preventDefault();
                console.log('>>> [종료] btn-reset-game clicked');
                await finishGame();
            });
            resetBtn.dataset.hasResetHandler = '1';
        } else {
            console.debug('btn-reset-game handler already bound');
        }
    } else {
        console.debug('btn-reset-game not found - skipping binding');
    }
});

async function setStartEpisode(event, epId) {
    event.stopPropagation();
    if (!confirm(`EP #${epId}를 이 루트의 시작점으로 설정하시겠습니까?`)) return;
    try {
        const currentRoute = writerState.routes.find(r => r.id === writerState.routeId);
        if (!currentRoute) throw new Error("루트 정보를 찾을 수 없습니다. 새로고침 후 다시 시도해주세요.");
        const updateData = {
            title: currentRoute.title,
            minAffectionRequired: currentRoute.minAffectionRequired,
            trueEndingThreshold: currentRoute.trueEndingThreshold,
            warningText: currentRoute.warningText,
            startEpisodeId: epId
        };
        await api(`/story/routes/${writerState.routeId}`, 'PUT', updateData);
        alert(`EP #${epId}가 시작점으로 설정되었습니다.`);
    } catch (e) { alert(e.message); }
}

async function deleteEpisode(event, epId) {
    event.stopPropagation();
    if (!confirm("정말 삭제하시겠습니까? (연결된 선택지도 모두 삭제됩니다)")) return;
    try {
        await api(`/story/episodes/${epId}`, 'DELETE');
        alert("삭제되었습니다.");
        loadWriterEpisodes(writerState.routeId);
    } catch (e) { alert(e.message); }
}

// 수정 모드 진입
function startEditEpisode(event, epId) {
    event.stopPropagation();
    const eps = (writerState && writerState.episodes) || [];
    const ep = eps.find(e => e.id === epId);
    if (!ep) return;

    editingEpId = epId;
    const form = document.getElementById('form-create-ep');
    if (!form) return;

    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) {
        submitBtn.innerText = "에피소드 수정 저장";
        submitBtn.style.background = "#ffc107";
        submitBtn.style.color = "#000";
    }

    if (!document.getElementById('btn-cancel-ep')) {
        const cancelBtn = document.createElement('button');
        cancelBtn.id = 'btn-cancel-ep';
        cancelBtn.type = 'button';
        cancelBtn.innerText = "취소";
        cancelBtn.style.marginLeft = "10px";
        cancelBtn.style.background = "#6c757d";
        cancelBtn.onclick = cancelEditEpisode;
        form.appendChild(cancelBtn);
    }

    const titleField = form.querySelector('[name=title]');
    const textField = form.querySelector('[name=text]');
    if (titleField) titleField.value = ep.title || '';
    if (textField) textField.value = ep.text || '';

    const chkEnding = form.querySelector('[name=isEnding]');
    if (chkEnding) chkEnding.checked = !!ep.isEnding;
    toggleEndingSelect && toggleEndingSelect();

    if (ep.isEnding) {
        const selEnding = form.querySelector('[name=endingType]');
        if (selEnding) selEnding.value = ep.endingType || '';
    }

    form.scrollIntoView({ behavior: 'smooth' });
}

// 통합: 에피소드 생성/수정 폼 핸들러 (중복 등록 방지)
(function registerEpisodeFormHandler() {
    const form = document.getElementById('form-create-ep');
    if (!form) return;
    if (form.dataset.hasEpHandler) return;
    form.dataset.hasEpHandler = '1';

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(form);
        const isEnding = fd.get('isEnding') === 'on';

        const data = {
            routeId: writerState.routeId,
            title: fd.get('title') ? fd.get('title').trim() : null,
            text: fd.get('text'),
            isEnding: isEnding,
            endingType: isEnding ? fd.get('endingType') : null
        };

        if (isEnding && !data.endingType) {
            alert("엔딩 타입을 선택해주세요.");
            return;
        }

        try {
            if (editingEpId) {
                await api(`/story/episodes/${editingEpId}`, 'PUT', data);
                alert("에피소드가 수정되었습니다.");
                editingEpId = null;
                cancelEditEpisode && cancelEditEpisode();
            } else {
                await api('/story/episodes', 'POST', data);
                alert("에피소드가 생성되었습니다.");
                form.reset();
                toggleEndingSelect && toggleEndingSelect();
            }

            loadWriterEpisodes((writerState && writerState.routeId) || data.routeId);
        } catch (err) {
            console.error('episode submit error', err);
            alert(err.message || '요청 중 오류가 발생했습니다.');
        }
    });
})();

// 수정 취소
function cancelEditEpisode() {
    editingEpId = null;
    const form = document.getElementById('form-create-ep');
    if (!form) return;
    form.reset();
    toggleEndingSelect && toggleEndingSelect();

    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) {
        submitBtn.innerText = "에피소드 추가";
        submitBtn.style.background = "";
        submitBtn.style.color = "";
    }

    const cancelBtn = document.getElementById('btn-cancel-ep');
    if (cancelBtn) cancelBtn.remove();
}

function toggleEndingSelect() {
    const chk = document.getElementById('chk-is-ending');
    const sel = document.getElementById('sel-ending-type');
    if (!chk || !sel) return;
    if (chk.checked) {
        sel.style.display = 'inline-block';
        sel.required = true;
    } else {
        sel.style.display = 'none';
        sel.required = false;
        sel.value = "";
    }
}

// --- 2-4. 선택지 관리 ---
async function loadWriterChoices(epId) {
    const list = document.getElementById('list-choice');
    if (!list) return;
    list.innerHTML = '';
    try {
        const choices = await api(`/story/episodes/${epId}/choices`);
        if (!choices || choices.length === 0) list.innerHTML = '<li style="background:#eee;">선택지가 없습니다.</li>';
        choices.forEach(ch => {
            const li = document.createElement('li');
            li.style.cursor = 'default';
            let branchInfo = ch.threshold ? `⚖️ ${ch.threshold}...` : `→ EP.${ch.nextEpIfPassId}`;
            li.innerHTML = `
                <div style="flex:1;">
                    <div><strong>${escapeHtml(ch.text)}</strong></div>
                    <div style="font-size:0.85em; color:#666;">${escapeHtml(branchInfo)}</div>
                </div>
                <button class="btn-sm" style="background:#dc3545; color:white;" onclick="deleteChoice(event, ${ch.id})">🗑️</button>
            `;
            list.appendChild(li);
        });
    } catch (e) { console.error(e); }
}

async function deleteChoice(event, choiceId) {
    event.stopPropagation();
    if (!confirm("이 선택지를 삭제하시겠습니까?")) return;
    try {
        await api(`/story/choices/${choiceId}`, 'DELETE');
        alert("삭제되었습니다.");
        loadWriterChoices(writerState.epId);
    } catch (e) { alert(e.message); }
}

document.getElementById('form-create-choice')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        text: fd.get('text'),
        minRequiredAffection: fd.get('minReq') ? parseInt(fd.get('minReq')) : null,
        affectionDelta: parseInt(fd.get('delta') || 0),
        threshold: fd.get('threshold') ? parseInt(fd.get('threshold')) : null,
        nextEpIfPassId: fd.get('nextEpIfPassId') ? parseInt(fd.get('nextEpIfPassId')) : null,
        nextEpIfFailId: fd.get('nextEpIfFailId') ? parseInt(fd.get('nextEpIfFailId')) : null
    };
    try {
        await api(`/story/episodes/${writerState.epId}/choices`, 'POST', data);
        e.target.reset();
        loadWriterChoices(writerState.epId);
    } catch (err) { alert(err.message); }
});

// ==========================================
// 3. 플레이어 로직
// ==========================================

async function loadCharacters() {
    console.log(">>> [로딩] 캐릭터 로딩 시작");
    const charScreen = document.getElementById('char-selection-screen');
    const container = document.getElementById('char-list');
    if (!charScreen || !container) {
        console.error(">>> [치명적 오류] HTML ID를 찾을 수 없음: char-selection-screen 또는 char-list");
        return;
    }
    container.style.display = '';
    document.getElementById('route-select-screen').style.display = 'none';
    document.getElementById('game-screen').style.display = 'none';
    document.getElementById('report-screen').style.display = 'none';
    charScreen.style.display = 'block';
    container.innerHTML = '<p style="padding:20px; font-weight:bold;">🔄 데이터를 불러오는 중입니다...</p>';
    try {
        const chars = await api(`/game/characters?_t=${Date.now()}`);
        console.log(">>> [로딩] 받아온 데이터:", chars);
        container.innerHTML = '';
        if (!chars || chars.length === 0) {
            container.innerHTML = '<p>😢 캐릭터 데이터가 없습니다. (DB를 확인해주세요)</p>';
            return;
        }
        let htmlBuilder = '';
        chars.forEach(c => {
            htmlBuilder += `
                <div class="char-card">
                    <h4>${escapeHtml(c.name)}</h4>
                    <p>${escapeHtml(c.description || '')}</p>
                    <div>
                        <button onclick="showPlayerRoutes(${c.id})">플레이</button>
                        <button onclick="openWriterForChar(${c.id}, '${escapeHtml(c.name)}')">작성</button>
                    </div>
                </div>
            `;
        });
        container.innerHTML = htmlBuilder;
        console.log(">>> [로딩] 화면 렌더링 완료");
        try {
            // 진행중 게임 체크 등 (옵션)
        } catch (e) { /* ignore */ }
    } catch (err) {
        console.error(">>> [에러]", err);
        container.innerHTML = `<p style="color:red; font-weight:bold;">⚠️ 로드 실패: ${escapeHtml(err.message)}</p>`;
    }
}

// javascript
function openWriterForChar(charId, charName) {
    // 권한 검사: 작가나 관리자만 접근 허용
    const allowed = currentUser && (currentUser.role === 'SCENARIO_WRITER' || currentUser.role === 'GAME_ADMIN');
    if (!allowed) {
        alert('작성 권한이 없습니다. 작가 계정으로 로그인하세요.');
        return;
    }

    // 상태 설정 후 명시적으로 작가 뷰로 전환
    writerState.charId = charId;
    writerState.charName = charName;

    // 뷰 전환과 내부 패널 초기화
    switchView('writer-view');
    resetWriterView('route');
}


function backToCharSelect() {
    document.getElementById('route-select-screen').style.display = 'none';
    document.getElementById('char-selection-screen').style.display = 'block';
}

async function showPlayerRoutes(charId) {
    document.getElementById('char-selection-screen').style.display = 'none';
    const routeScreen = document.getElementById('route-select-screen');
    if (!routeScreen) return;
    routeScreen.style.display = 'block';
    const list = document.getElementById('player-route-list');
    if (!list) return;
    list.innerHTML = 'Loading...';
    try {
        const routes = await api(`/story/characters/${charId}/routes`);
        list.innerHTML = '';
        if (!routes || routes.length === 0) {
            list.innerHTML = '<p>플레이 가능한 루트가 없습니다.</p>';
            return;
        }
        routes.forEach(r => {
            const li = document.createElement('li');
            li.innerHTML = `
                <div><strong>${escapeHtml(r.title)}</strong></div>
                <div><button onclick="startGame(${r.id})">시작</button></div>
            `;
            list.appendChild(li);
        });
    } catch (err) {
        alert("루트 목록 로드 실패: " + err.message);
        backToCharSelect();
    }
}

async function startGame(routeId) {
    try {
        const state = await api(`/game/start?routeId=${routeId}`, 'POST');
        document.getElementById('route-select-screen').style.display = 'none';
        renderGameScreen(state);
    } catch (err) { alert(err.message); }
}

// renderGameScreen의 선택지 렌더링(문자열 innerHTML 대신 안전한 생성)
function renderGameScreen(state) {
    document.getElementById('char-list').style.display = 'none';
    const screen = document.getElementById('game-screen');
    screen.style.display = 'block';
    document.getElementById('report-screen').style.display = 'none';
    const titleText = (state.epTitle && state.epTitle.trim()) ? state.epTitle.trim() : `Episode #${state.epId}`;
    document.getElementById('ep-title').innerText = titleText;
    document.getElementById('ep-text').innerText = state.epText;
    document.getElementById('current-affection').innerText = state.affection;
    const choiceArea = document.getElementById('choice-area');
    choiceArea.innerHTML = '';

    if (state.isEnding) {
        const endBtn = document.createElement('button');
        endBtn.innerText = `[${state.endingLabel || 'ENDING'}] 리포트 보기`;
        endBtn.onclick = showLatestReport;
        choiceArea.appendChild(endBtn);
        return;
    }

    state.choices.forEach(ch => {
        // 안전: ch.id가 유효한지 확인
        const btn = document.createElement('button');
        btn.className = 'choice-btn';
        // 버튼 텍스트 (XSS 방지)
        const label = ch.text ? ch.text : `선택 ${ch.id || ''}`;
        btn.innerText = label;
        // 클릭 핸들러은 클로저로 ch.id를 전달
        btn.onclick = () => {
            makeChoice(ch.id);
        };
        // 잠금 상태 처리 예시
        if (ch.minRequiredAffection && state.affection < ch.minRequiredAffection) {
            btn.classList.add('locked');
            btn.disabled = true;
        }
        choiceArea.appendChild(btn);
    });
}

async function makeChoice(choiceId) {
    // 방어: undefined/null/'undefined'/NaN 등 차단
    if (choiceId === undefined || choiceId === null || String(choiceId) === 'undefined' || Number.isNaN(Number(choiceId))) {
        console.error('Invalid choiceId passed to makeChoice:', choiceId);
        alert('잘못된 선택입니다. 다시 시도해주세요.');
        return;
    }
    try {
        const newState = await api(`/game/choose?choiceId=${encodeURIComponent(choiceId)}`, 'POST');
        renderGameScreen(newState);
    } catch (err) {
        alert(err.message);
    }
}

async function showLatestReport() {
    try {
        const report = await api('/game/report/latest');
        document.getElementById('game-screen').style.display = 'none';
        const repDiv = document.getElementById('report-screen');
        repDiv.style.display = 'block';
        let html = `<h4>${escapeHtml(report.characterName)} - ${escapeHtml(report.routeTitle)}</h4>`;
        const rawType = report.endingType ? report.endingType : null;
        const normalized = rawType ? (String(rawType).toUpperCase() === 'TRUE' ? 'GOOD' : String(rawType).toUpperCase()) : null;
        const displayLabel = normalized ? formatEndingLabel(normalized) : "진행 중";
        html += `<p>결과: <strong style="color:${normalized==='BAD'?'red':'blue'}">${escapeHtml(displayLabel)}</strong></p>`;
        html += `<p>최종 호감도: ${report.finalAffection}</p>`;
        html += `<h5>플레이 로그</h5><ul>`;
        report.steps.forEach(s => {
            html += `<li>EP: ${s.epId} → 선택: ${escapeHtml(s.choiceText || '-')} (호감도 ${s.affectionBefore} -> ${s.affectionAfter})</li>`;
        });
        html += `</ul>`;
        document.getElementById('report-content').innerHTML = html;
    } catch (err) { alert(err.message); }
}

function formatEndingLabel(endingType) {
    if (!endingType) return '';
    const key = String(endingType).toUpperCase();
    if (key === 'TRUE' || key === 'GOOD') return 'GOOD (해피엔딩)';
    if (key === 'NORMAL') return 'NORMAL (노말)';
    if (key === 'BAD') return 'BAD (배드)';
    return escapeHtml(key);
}

function resetGameUI() {
    document.getElementById('game-screen').style.display = 'none';
    document.getElementById('report-screen').style.display = 'none';
    document.getElementById('char-list').style.display = 'grid';
}

// ==========================================
// 4. 관리자 로직
// ==========================================

async function loadRoleRequests() {
    try {
        const list = await api('/admin/role-requests');
        const tbody = document.querySelector('#role-req-table tbody');
        if (!tbody) return;
        tbody.innerHTML = '';
        list.forEach(req => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${req.id}</td>
                <td>${escapeHtml(req.email)}</td>
                <td>${escapeHtml(req.requestedRole)}</td>
                <td>
                    <button onclick="handleRoleReq(${req.id}, 'approve')">승인</button>
                    <button onclick="handleRoleReq(${req.id}, 'reject')">거절</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) { console.error(e); }
}

async function handleRoleReq(id, action) {
    try {
        await api(`/admin/role-requests/${id}/${action}`, 'POST');
        loadRoleRequests();
    } catch (err) { alert(err.message); }
}

async function loadAnalyticsRoutes() {
    try {
        const routes = await api('/admin/analytics/routes');
        const sel = document.getElementById('analytics-route-select');
        if (!sel) return;
        sel.innerHTML = '<option value="">통계를 볼 루트 선택...</option>';
        routes.forEach(r => {
            sel.innerHTML += `<option value="${r.id}">[${r.id}] ${escapeHtml(r.title)} (${escapeHtml(r.charName)})</option>`;
        });
    } catch (e) {}
}

async function loadRouteStats(routeId) {
    if (!routeId) {
        document.getElementById('analytics-result').style.display = 'none';
        return;
    }
    document.getElementById('analytics-result').style.display = 'block';
    try {
        const summary = await api(`/admin/analytics/routes/${routeId}/summary`);
        document.getElementById('stats-summary').innerHTML = `
            <ul>
                <li>총 방문: ${summary.totalVisits}</li>
                <li>총 클리어: ${summary.totalClears} (Good: ${summary.trueEndingClears}, Normal: ${summary.normalEndingClears}, Bad: ${summary.badEndingClears})</li>
            </ul>
        `;
        const eps = await api(`/admin/analytics/routes/${routeId}/episodes`);
        const epBody = document.querySelector('#stats-ep-table tbody');
        if (epBody) {
            epBody.innerHTML = '';
            eps.forEach(ep => {
                epBody.innerHTML += `<tr><td>${ep.epId}</td><td>${escapeHtml(ep.epText.substring(0,20))}...</td><td>${ep.visitcout}</td><td>${ep.clearCout}</td><td>${(1 - ep.clearRate).toFixed(2)}</td></tr>`;
            });
        }
        const choices = await api(`/admin/analytics/routes/${routeId}/choices`);
        const chBody = document.querySelector('#stats-ch-table tbody');
        if (chBody) {
            chBody.innerHTML = '';
            choices.forEach(ch => {
                chBody.innerHTML += `<tr><td>${ch.choiceId}</td><td>${escapeHtml(ch.choiceText)}</td><td>${ch.pickCount}</td><td>${ch.passCount}</td><td>${ch.failCount}</td></tr>`;
            });
        }
    } catch (err) { alert('통계 로드 실패: ' + err.message); }
}