const API_BASE = '/api';

/**
 * 공통 Fetch Wrapper (Session Cookie 포함)
 */
async function api(endpoint, method = 'GET', body = null) {
    const options = {
        method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
    };
    if (body) options.body = JSON.stringify(body);

    const res = await fetch(API_BASE + endpoint, options);
    if (!res.ok) {
        const err = await res.json().catch(() => ({ message: res.statusText }));
        throw new Error(err.message || `Error ${res.status}`);
    }
    // 204 No Content 처리
    if (res.status === 204) return null;
    return res.json().catch(() => null); // 응답이 비어있을 경우 대비
}

// === 전역 상태 ===
let currentUser = null;

// === Writer View 상태 관리 (계층형 이동용) ===
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

document.addEventListener('DOMContentLoaded', () => {
    checkLogin();
});

// ==========================================
// 1. 인증 로직 (AuthController)
// ==========================================

function showAuthTab(tab) {
    // 1. 폼 화면 전환
    document.getElementById('login-tab').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('signup-tab').style.display = tab === 'signup' ? 'block' : 'none';

    // 2. 탭 버튼 스타일 초기화
    const buttons = document.querySelectorAll('.tab-btn');
    buttons.forEach(b => b.classList.remove('active'));

    // 3. 현재 탭 버튼 활성화
    if (tab === 'login') {
        buttons[0].classList.add('active');
    } else {
        buttons[1].classList.add('active');
    }
}

// 로그인 체크 (/auth/me)
async function checkLogin() {
    try {
        const user = await api('/auth/me');
        if (user) handleLoginSuccess(user);
    } catch (e) {
        console.log('Not logged in');
        document.getElementById('auth-section').style.display = 'block';
    }
}

// 로그인
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        const res = await api('/auth/login', 'POST', { email, password });
        handleLoginSuccess({ name: res.name, role: res.role });
    } catch (err) { alert('로그인 실패: ' + err.message); }
});

// 회원가입
document.getElementById('signup-form').addEventListener('submit', async (e) => {
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

// 로그아웃
document.getElementById('logout-btn').addEventListener('click', async () => {
    await api('/auth/logout', 'POST');
    location.reload();
});

function handleLoginSuccess(user) {
    currentUser = user;
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('dashboard-section').style.display = 'block';
    document.getElementById('user-info').style.display = 'block';
    document.getElementById('welcome-msg').innerText = `${user.name} (${user.role})`;

    // 역할별 탭 표시 제어
    const role = user.role;

    if (['SCENARIO_WRITER', 'GAME_ADMIN'].includes(role)) {
        document.getElementById('nav-writer').style.display = 'inline-block';
    }
    if (['BALANCE_TUNER', 'GAME_ADMIN'].includes(role)) {
        document.getElementById('nav-admin').style.display = 'inline-block';
    }

    // 기본 뷰 로드 (플레이어 화면)
    switchView('player-view');
    loadCharacters();
}

function switchView(viewId) {
    document.querySelectorAll('.view-panel').forEach(el => el.style.display = 'none');
    document.getElementById(viewId).style.display = 'block';

    // Admin 뷰 진입 시 데이터 자동 로드
    if(viewId === 'admin-view') {
        loadRoleRequests();
        loadAnalyticsRoutes();
        loadAdminLogs(); // [추가] 로그 로드 호출!
    }
    if(viewId === 'writer-view') {
        resetWriterView('char');
    }
}

async function loadAdminLogs() {
    const tbody = document.querySelector('#admin-log-table tbody');
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">로딩 중...</td></tr>';

    try {
        // ReportForBalanceController에 만든 /logs API 호출
        const logs = await api('/admin/analytics/logs');

        tbody.innerHTML = '';
        if (!logs || logs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">기록된 로그가 없습니다.</td></tr>';
            return;
        }

        logs.forEach(log => {
            const tr = document.createElement('tr');

            // 날짜 포맷팅 (YYYY-MM-DD HH:mm:ss)
            const date = new Date(log.createdAt).toLocaleString();

            // 액션별 스타일 클래스 지정
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
                <td>${log.description}</td>
            `;
            tbody.appendChild(tr);
        });

    } catch(err) {
        console.error(err);
        tbody.innerHTML = `<tr><td colspan="6" style="color:red; text-align:center;">로그 로드 실패: ${err.message}</td></tr>`;
    }
}

// ==========================================
// 2. 시나리오 작가 로직 (계층형 UI & 단일 경로)
// ==========================================

// 네비게이션 및 뷰 초기화
// level: 'char' | 'route' | 'ep' | 'choice'
function resetWriterView(level) {
    // 1. 모든 패널 숨김
    ['panel-char', 'panel-route', 'panel-ep', 'panel-choice'].forEach(id => {
        document.getElementById(id).style.display = 'none';
    });

    // 2. Breadcrumb(경로) 초기화
    document.getElementById('nav-route').style.display = 'none';
    document.getElementById('nav-ep').style.display = 'none';
    document.getElementById('nav-choice').style.display = 'none';

    // 3. 레벨별 뷰 활성화
    if (level === 'char') {
        document.getElementById('panel-char').style.display = 'block';
        loadWriterCharacters();
    }
    else if (level === 'route') {
        document.getElementById('panel-route').style.display = 'block';
        document.getElementById('nav-route').style.display = 'inline';
        document.getElementById('selected-char-name').innerText = ` - ${writerState.charName}`;
        loadWriterRoutes(writerState.charId);
    }
    else if (level === 'ep') {
        document.getElementById('panel-ep').style.display = 'block';
        document.getElementById('nav-route').style.display = 'inline';
        document.getElementById('nav-ep').style.display = 'inline';
        document.getElementById('selected-route-title').innerText = ` - ${writerState.routeTitle}`;
        loadWriterEpisodes(writerState.routeId);
    }
    else if (level === 'choice') {
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

    // 초기화
    selPass.innerHTML = '<option value="">성공 시 이동할 에피소드...</option>';
    selFail.innerHTML = '<option value="">실패 시 이동할 에피소드...</option>';

    try {
        const eps = await api(`/story/routes/${writerState.routeId}/episodes`);

        eps.forEach(ep => {
            const label = `[EP.${ep.id}] ${ep.text.substring(0, 15)}...${ep.isEnding ? ` (${ep.endingType})` : ''}`;

            // Pass 옵션 추가
            const opt1 = document.createElement('option');
            opt1.value = ep.id;
            opt1.innerText = label;
            selPass.appendChild(opt1);

            // Fail 옵션 추가 (복사해서 사용)
            const opt2 = document.createElement('option');
            opt2.value = ep.id;
            opt2.innerText = label;
            selFail.appendChild(opt2);
        });
    } catch(e) { console.error(e); }
}

// --- 2-1. 캐릭터 관리 ---
async function loadWriterCharacters() {
    const list = document.getElementById('list-char');
    list.innerHTML = '';
    try {
        const chars = await api('/game/characters');
        chars.forEach(c => {
            const li = document.createElement('li');
            li.innerHTML = `
                <div><strong>${c.name}</strong> <small>(${c.gender}, ${c.personality})</small></div>
                <div>
                    <span style="font-size:0.8em; color:#888; margin-right:10px; cursor:pointer;">ID: ${c.id} &gt;</span>
                    <button class="btn-sm" style="background:#dc3545; color:white;" onclick="deleteCharacter(event, ${c.id})">🗑️</button>
                </div>
            `;
            // 클릭 시 이동 (버튼 제외)
            li.onclick = (e) => {
                if(e.target.tagName === 'BUTTON') return;
                writerState.charId = c.id;
                writerState.charName = c.name;
                resetWriterView('route');
            };
            list.appendChild(li);
        });
    } catch(e) { console.error(e); }
}

async function deleteCharacter(event, charId) {
    event.stopPropagation();
    if(!confirm("캐릭터를 삭제하시겠습니까?\n포함된 모든 루트와 에피소드가 함께 삭제됩니다!")) return;
    try {
        await api(`/story/characters/${charId}`, 'DELETE');
        alert("삭제되었습니다.");
        loadWriterCharacters(); // 목록 갱신
    } catch(e) { alert(e.message); }
}

document.getElementById('form-create-char').addEventListener('submit', async (e) => {
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
    } catch(err) { alert(err.message); }
});

// --- 2-2. 루트 관리 ---
async function loadWriterRoutes(charId) {
    const list = document.getElementById('list-route');
    list.innerHTML = '';
    try {
        const routes = await api(`/story/characters/${charId}/routes`);

        writerState.routes = routes;
        if(routes.length === 0) list.innerHTML = '<li style="cursor:default; background:#eee;">생성된 루트가 없습니다.</li>';

        routes.forEach(r => {
            const li = document.createElement('li');
            li.innerHTML = `
                <div><strong>${r.title}</strong></div>
                <div>
                    <span style="font-size:0.8em; color:#888; margin-right:10px; cursor:pointer;">ID: ${r.id} &gt;</span>
                    <button class="btn-sm" style="background:#dc3545; color:white;" onclick="deleteRoute(event, ${r.id})">🗑️</button>
                </div>
            `;
            li.onclick = (e) => {
                if(e.target.tagName === 'BUTTON') return;
                writerState.routeId = r.id;
                writerState.routeTitle = r.title;
                resetWriterView('ep');
            };
            list.appendChild(li);
        });
    } catch(e) { console.error(e); }
}

// [추가] 루트 삭제 함수
async function deleteRoute(event, routeId) {
    event.stopPropagation();
    if(!confirm("루트를 삭제하시겠습니까?\n포함된 모든 에피소드가 함께 삭제됩니다!")) return;
    try {
        await api(`/story/routes/${routeId}`, 'DELETE');
        alert("삭제되었습니다.");
        loadWriterRoutes(writerState.charId); // 목록 갱신
    } catch(e) { alert(e.message); }
}
document.getElementById('form-create-route').addEventListener('submit', async (e) => {
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
    } catch(err) { alert(err.message); }
});

// --- 2-3. 에피소드 관리 ---
// [교체] 에피소드 목록 로드 (수정 버튼 포함)
async function loadWriterEpisodes(routeId) {
    const container = document.getElementById('list-ep');
    container.innerHTML = '';

    try {
        // 1. API 호출
        const eps = await api(`/story/routes/${routeId}/episodes`);

        // 2. 수정 기능을 위해 상태에 저장해둠
        writerState.episodes = eps;

        if(eps.length === 0) {
            container.innerHTML = '<p style="padding:10px; color:#999;">에피소드가 없습니다.</p>';
            return;
        }

        eps.forEach(ep => {
            const div = document.createElement('div');
            // 스타일 클래스: 엔딩이면 테두리 색 다르게
            div.className = `ep-card ${ep.isEnding ? 'ending' : ''}`;

            // HTML 구성: [EDIT] 버튼 추가
            div.innerHTML = `
                <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:5px;">
                    <span style="font-weight:bold;">EP #${ep.id}</span>
                    <button class="btn-sm" style="background:#00b894; color:white; margin-right:5px;" onclick="setStartEpisode(event, ${ep.id})">🚩시작점</button>
                    <button class="btn-sm" style="background:#ffc107; color:#333; border:none; padding:2px 8px; border-radius:4px; font-size:0.8em; cursor:pointer;" onclick="startEditEpisode(event, ${ep.id})">✏️수정</button>
                    <button class="btn-sm" style="background:#dc3545; color:white;" onclick="deleteEpisode(event, ${ep.id})">🗑️삭제</button>
                </div>
                <div style="font-size:0.85em; color:#555; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">
                    ${ep.text}
                </div>
                ${ep.isEnding ? `<div style="color:red; font-size:0.8em; margin-top:5px; font-weight:bold;">[${ep.endingType}]</div>` : ''}
            `;

            // 카드 본문 클릭 시: 선택지 관리로 이동 (드릴다운)
            div.onclick = () => {
                writerState.epId = ep.id;
                writerState.epText = ep.text;
                resetWriterView('choice');
            };

            container.appendChild(div);
        });
    } catch(e) { console.error(e); }
}

async function setStartEpisode(event, epId) {
    event.stopPropagation();
    if(!confirm(`EP #${epId}를 이 루트의 시작점으로 설정하시겠습니까?`)) return;

    try {
        // 1. 아까 저장해둔 목록에서 현재 루트 정보를 찾습니다.
        const currentRoute = writerState.routes.find(r => r.id === writerState.routeId);

        if (!currentRoute) {
            throw new Error("루트 정보를 찾을 수 없습니다. 새로고침 후 다시 시도해주세요.");
        }

        // 2. 기존 정보는 그대로 유지하고, startEpisodeId만 변경합니다.
        const updateData = {
            title: currentRoute.title,
            minAffectionRequired: currentRoute.minAffectionRequired,
            trueEndingThreshold: currentRoute.trueEndingThreshold,
            warningText: currentRoute.warningText,

            startEpisodeId: epId // [변경] 이것만 바꿈!
        };

        // 3. 업데이트 요청 전송
        await api(`/story/routes/${writerState.routeId}`, 'PUT', updateData);

        alert(`EP #${epId}가 시작점으로 설정되었습니다.`);

        // (선택) 목록을 갱신해서 변경 사항을 다시 받아오려면:
        // loadWriterRoutes(writerState.charId);

    } catch(e) { alert(e.message); }
}

async function deleteEpisode(event, epId) {
    event.stopPropagation();
    if(!confirm("정말 삭제하시겠습니까? (연결된 선택지도 모두 삭제됩니다)")) return;

    try {
        await api(`/story/episodes/${epId}`, 'DELETE');
        alert("삭제되었습니다.");
        loadWriterEpisodes(writerState.routeId); // 목록 갱신
    } catch(e) { alert(e.message); }
}

// [추가] 수정 모드 진입
function startEditEpisode(event, epId) {
    event.stopPropagation(); // 부모(카드) 클릭 이벤트 전파 방지 -> 페이지 이동 막음

    // 1. 저장해둔 목록에서 데이터 찾기
    const ep = writerState.episodes.find(e => e.id === epId);
    if (!ep) return;

    // 2. UI 업데이트 (수정 모드)
    editingEpId = epId;
    const form = document.getElementById('form-create-ep');
    const submitBtn = form.querySelector('button[type="submit"]');

    // 버튼 스타일 변경
    submitBtn.innerText = "에피소드 수정 저장";
    submitBtn.style.background = "#ffc107"; // 노란색 (Warning 느낌)
    submitBtn.style.color = "#000";

    // 취소 버튼 추가 (없을 때만)
    if (!document.getElementById('btn-cancel-ep')) {
        const cancelBtn = document.createElement('button');
        cancelBtn.id = 'btn-cancel-ep';
        cancelBtn.type = 'button';
        cancelBtn.innerText = "취소";
        cancelBtn.style.marginLeft = "10px";
        cancelBtn.style.background = "#6c757d"; // 회색
        cancelBtn.onclick = cancelEditEpisode;
        form.appendChild(cancelBtn);
    }

    // 3. 폼에 데이터 채우기
    form.querySelector('[name=text]').value = ep.text;

    const chkEnding = form.querySelector('[name=isEnding]');
    chkEnding.checked = ep.isEnding;

    // 엔딩 여부에 따라 드롭다운 표시/숨김 처리
    toggleEndingSelect();

    if (ep.isEnding) {
        form.querySelector('[name=endingType]').value = ep.endingType;
    }

    // 4. 입력 폼으로 스크롤 이동
    form.scrollIntoView({ behavior: 'smooth' });
}

// [추가] 수정 취소
function cancelEditEpisode() {
    editingEpId = null;
    const form = document.getElementById('form-create-ep');

    // 폼 초기화
    form.reset();
    toggleEndingSelect(); // 드롭다운 숨기기

    // 버튼 원상복구
    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.innerText = "에피소드 추가";
    submitBtn.style.background = ""; // 원래 색으로
    submitBtn.style.color = "";

    // 취소 버튼 제거
    const cancelBtn = document.getElementById('btn-cancel-ep');
    if (cancelBtn) cancelBtn.remove();
}

function toggleEndingSelect() {
    const chk = document.getElementById('chk-is-ending');
    const sel = document.getElementById('sel-ending-type');

    if (chk.checked) {
        sel.style.display = 'inline-block'; // 보이기
        sel.required = true;                // 엔딩이면 타입 선택 필수
    } else {
        sel.style.display = 'none';         // 숨기기
        sel.required = false;               // 필수 해제
        sel.value = "";                     // 선택값 초기화
    }
}
// [교체] 에피소드 생성/수정 폼 제출 이벤트
document.getElementById('form-create-ep').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const isEnding = fd.get('isEnding') === 'on';

    const data = {
        routeId: writerState.routeId, // 생성 시 필요 (수정 시엔 무시됨)
        text: fd.get('text'),
        isEnding: isEnding,
        endingType: isEnding ? fd.get('endingType') : null
    };

    // 유효성 검사
    if (isEnding && !data.endingType) {
        alert("엔딩 타입을 선택해주세요.");
        return;
    }

    try {
        if (editingEpId) {
            // [수정 모드] PUT 요청
            await api(`/story/episodes/${editingEpId}`, 'PUT', data);
            alert("수정되었습니다.");
            cancelEditEpisode(); // 수정 모드 종료 및 폼 초기화
        } else {
            // [생성 모드] POST 요청
            await api('/story/episodes', 'POST', data);
            alert("생성되었습니다.");
            e.target.reset();
            toggleEndingSelect();
        }

        // 목록 갱신
        loadWriterEpisodes(writerState.routeId);

    } catch(err) {
        alert(err.message);
    }
});

// --- 2-4. 선택지 관리 (단일 경로) ---
async function loadWriterChoices(epId) {
    const list = document.getElementById('list-choice');
    list.innerHTML = '';
    try {
        const choices = await api(`/story/episodes/${epId}/choices`);
        if(choices.length === 0) list.innerHTML = '<li style="background:#eee;">선택지가 없습니다.</li>';

        choices.forEach(ch => {
            const li = document.createElement('li');
            li.style.cursor = 'default';

            // ... (기존 내용 표시 로직) ...
            let branchInfo = ch.threshold ? `⚖️ ${ch.threshold}...` : `→ EP.${ch.nextEpIfPassId}`;

            li.innerHTML = `
                <div style="flex:1;">
                    <strong>${ch.text}</strong>
                    <div style="font-size:0.85em; color:#666;">${branchInfo}</div>
                </div>
                <button class="btn-sm" style="background:#dc3545; color:white;" onclick="deleteChoice(event, ${ch.id})">🗑️</button>
            `;
            list.appendChild(li);
        });
    } catch(e) { console.error(e); }
}

// [추가] 선택지 삭제 함수
async function deleteChoice(event, choiceId) {
    event.stopPropagation();
    if(!confirm("이 선택지를 삭제하시겠습니까?")) return;
    try {
        await api(`/story/choices/${choiceId}`, 'DELETE');
        alert("삭제되었습니다.");
        loadWriterChoices(writerState.epId); // 목록 갱신
    } catch(e) { alert(e.message); }
}
document.getElementById('btn-reset-game').addEventListener('click', async () => {
    if (!confirm('정말로 게임을 리셋하시겠습니까?\n현재 진행 중인 데이터가 초기화됩니다.')) {
        return;
    }

    try {
        // 1. 새로 만드신 리셋 API 호출
        await api('/game/reset', 'POST');

        alert('게임이 초기화되었습니다. 캐릭터 선택 화면으로 이동합니다.');

        // 2. 화면 초기화 (게임 화면 닫기 & 캐릭터 목록 다시 로드)
        resetGameUI();
        loadCharacters();

    } catch (err) {
        alert('리셋 실패: ' + err.message);
    }
});

document.getElementById('form-create-choice').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);

    // DTO 필드명에 맞춰서 데이터 구성
    const data = {
        text: fd.get('text'),
        minRequiredAffection: fd.get('minReq') ? parseInt(fd.get('minReq')) : null,
        affectionDelta: parseInt(fd.get('delta') || 0),

        // 분기 관련 데이터
        threshold: fd.get('threshold') ? parseInt(fd.get('threshold')) : null,
        nextEpIfPassId: fd.get('nextEpIfPassId') ? parseInt(fd.get('nextEpIfPassId')) : null,
        nextEpIfFailId: fd.get('nextEpIfFailId') ? parseInt(fd.get('nextEpIfFailId')) : null
    };

    try {
        await api(`/story/episodes/${writerState.epId}/choices`, 'POST', data);
        e.target.reset();
        // 다시 로드해서 목록 갱신
        loadWriterChoices(writerState.epId);
    } catch(err) { alert(err.message); }
});


// ==========================================
// 3. 플레이어 로직 (GameController)
// ==========================================

async function loadCharacters() {
    // 화면 초기화: 캐릭터 선택만 보이고 나머지는 숨김
    document.getElementById('char-selection-screen').style.display = 'block';
    document.getElementById('route-select-screen').style.display = 'none';
    document.getElementById('game-screen').style.display = 'none';
    document.getElementById('report-screen').style.display = 'none';

    try {
        const chars = await api('/game/characters');
        const container = document.getElementById('char-list');
        container.innerHTML = '';


        chars.forEach(c => {
            const div = document.createElement('div');
            div.className = 'char-card';
            div.innerHTML = `
                <h4>${c.name}</h4>
                <p style="font-size:0.9em; color:#555;">${c.gender} / ${c.personality}</p>
                <p style="font-weight:bold; color:#e91e63; margin: 5px 0;">
                    🔥 인기: ${c.popularityScore}
                </p>
                <button onclick="showPlayerRoutes(${c.id})">선택하기</button>
            `;
            container.appendChild(div);
        });

        // 진행 중인 게임이 있으면 바로 게임 화면으로 (기존 로직 유지)
        try {
            const current = await api('/game/current');
            renderGameScreen(current);
            document.getElementById('char-selection-screen').style.display = 'none'; // 게임 중이면 숨김
        } catch(e) {}

    } catch(err) { console.error(err); }
}

// [추가] 뒤로 가기 버튼
function backToCharSelect() {
    document.getElementById('route-select-screen').style.display = 'none';
    document.getElementById('char-selection-screen').style.display = 'block';
}

// [추가] 해당 캐릭터의 루트 목록 보여주기
async function showPlayerRoutes(charId) {
    document.getElementById('char-selection-screen').style.display = 'none';
    const routeScreen = document.getElementById('route-select-screen');
    routeScreen.style.display = 'block';

    const list = document.getElementById('player-route-list');
    list.innerHTML = 'Loading...';

    try {
        // 기존 StoryController의 API 재활용 (GET /api/story/characters/{id}/routes)
        const routes = await api(`/story/characters/${charId}/routes`);
        list.innerHTML = '';

        if (routes.length === 0) {
            list.innerHTML = '<p>플레이 가능한 루트가 없습니다.</p>';
            return;
        }

        routes.forEach(r => {
            const li = document.createElement('li');
            li.innerHTML = `
                <div>
                    <strong>${r.title}</strong>
                    <div style="font-size:0.8em; color:#666;">${r.warningText || '설명 없음'}</div>
                </div>
                <button class="btn-sm" onclick="startGame(${r.id})">시작</button>
            `;
            list.appendChild(li);
        });
    } catch(err) {
        alert("루트 목록 로드 실패: " + err.message);
        backToCharSelect();
    }
}
async function startGame(routeId) {
    try {
        // 백엔드 API 호출 시 파라미터 이름 변경 (routeId)
        const state = await api(`/game/start?routeId=${routeId}`, 'POST');

        // 화면 전환
        document.getElementById('route-select-screen').style.display = 'none';
        renderGameScreen(state);
    } catch(err) { alert(err.message); }
}

function renderGameScreen(state) {
    document.getElementById('char-list').style.display = 'none';
    const screen = document.getElementById('game-screen');
    screen.style.display = 'block';
    document.getElementById('report-screen').style.display = 'none';

    document.getElementById('ep-title').innerText = `Episode #${state.epId}`;
    document.getElementById('ep-text').innerText = state.epText;
    document.getElementById('current-affection').innerText = state.affection;

    const choiceArea = document.getElementById('choice-area');
    choiceArea.innerHTML = '';

    if (state.isEnding) {
        const endBtn = document.createElement('button');
        endBtn.innerText = `[${state.endingLabel || 'ENDING'}] 리포트 보기`;
        endBtn.onclick = showLatestReport;
        choiceArea.appendChild(endBtn);
    } else {
        state.choices.forEach(ch => {
            const btn = document.createElement('button');
            btn.className = `choice-btn ${ch.locked ? 'locked' : ''}`;
            btn.innerText = ch.text + (ch.locked ? ` (🔒 호감도 ${ch.minRequiredAffection} 필요)` : '');
            if (!ch.locked) {
                btn.onclick = () => makeChoice(ch.id);
            }
            choiceArea.appendChild(btn);
        });
    }
}

async function makeChoice(choiceId) {
    try {
        const newState = await api(`/game/choose?choiceId=${choiceId}`, 'POST');
        renderGameScreen(newState);
    } catch(err) { alert(err.message); }
}

async function showLatestReport() {
    try {
        const report = await api('/game/report/latest');
        document.getElementById('game-screen').style.display = 'none';
        const repDiv = document.getElementById('report-screen');
        repDiv.style.display = 'block';

        let html = `<h4>${report.characterName} - ${report.routeTitle}</h4>`;

        const resultType = report.endingType ? report.endingType : "진행 중";
        html += `<p>결과: <strong style="color:${resultType==='BAD'?'red':'blue'}">${resultType}</strong></p>`;
        html += `<p>최종 호감도: ${report.finalAffection}</p>`;
        html += `<h5>플레이 로그</h5><ul>`;
        report.steps.forEach(s => {
            html += `<li>EP: ${s.epId} → 선택: ${s.choiceText || '-'} (호감도 ${s.affectionBefore} -> ${s.affectionAfter})</li>`;
        });
        html += `</ul>`;
        document.getElementById('report-content').innerHTML = html;
    } catch(err) { alert(err.message); }
}

function resetGameUI() {
    document.getElementById('game-screen').style.display = 'none';
    document.getElementById('report-screen').style.display = 'none';
    document.getElementById('char-list').style.display = 'grid';
}


// ==========================================
// 4. 관리자 로직 (AdminController)
// ==========================================

// 권한 요청 목록
async function loadRoleRequests() {
    try {
        const list = await api('/admin/role-requests');
        const tbody = document.querySelector('#role-req-table tbody');
        tbody.innerHTML = '';
        list.forEach(req => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${req.id}</td>
                <td>${req.email}</td>
                <td>${req.requestedRole}</td>
                <td>
                    <button class="btn-sm" onclick="handleRoleReq(${req.id}, 'approve')">승인</button>
                    <button class="btn-sm btn-danger" onclick="handleRoleReq(${req.id}, 'reject')">거절</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch(e) { console.error(e); }
}

async function handleRoleReq(id, action) {
    try {
        await api(`/admin/role-requests/${id}/${action}`, 'POST');
        loadRoleRequests();
    } catch(err) { alert(err.message); }
}

// 통계용 루트 목록
async function loadAnalyticsRoutes() {
    try {
        const routes = await api('/admin/analytics/routes');
        const sel = document.getElementById('analytics-route-select');
        sel.innerHTML = '<option value="">통계를 볼 루트 선택...</option>';
        routes.forEach(r => {
            sel.innerHTML += `<option value="${r.id}">[${r.id}] ${r.title} (${r.charName})</option>`;
        });
    } catch(e) {}
}

// 통계 상세 조회
async function loadRouteStats(routeId) {
    if(!routeId) {
        document.getElementById('analytics-result').style.display = 'none';
        return;
    }
    document.getElementById('analytics-result').style.display = 'block';

    try {
        // Summary
        const summary = await api(`/admin/analytics/routes/${routeId}/summary`);
        document.getElementById('stats-summary').innerHTML = `
            <ul>
                <li>총 방문: ${summary.totalVisits}</li>
                <li>총 클리어: ${summary.totalClears} (True: ${summary.trueEndingClears}, Normal: ${summary.normalEndingClears}, Bad: ${summary.badEndingClears})</li>
            </ul>
        `;

        // Episodes
        const eps = await api(`/admin/analytics/routes/${routeId}/episodes`);
        const epBody = document.querySelector('#stats-ep-table tbody');
        epBody.innerHTML = '';
        eps.forEach(ep => {
            epBody.innerHTML += `<tr><td>${ep.epId}</td><td>${ep.epText.substring(0,20)}...</td><td>${ep.visitcout}</td><td>${ep.clearCout}</td><td>${(1 - ep.clearRate).toFixed(2)}</td></tr>`;
        });

        // Choices
        const choices = await api(`/admin/analytics/routes/${routeId}/choices`);
        const chBody = document.querySelector('#stats-ch-table tbody');
        chBody.innerHTML = '';
        choices.forEach(ch => {
            chBody.innerHTML += `<tr><td>${ch.choiceId}</td><td>${ch.choiceText}</td><td>${ch.pickCount}</td><td>${ch.passCount}</td><td>${ch.failCount}</td></tr>`;
        });

    } catch(err) { alert('통계 로드 실패: ' + err.message); }
}