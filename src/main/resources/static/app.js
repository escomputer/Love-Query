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
    epText: ''
};

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

    // 뷰 진입 시 초기 데이터 로드
    if(viewId === 'admin-view') {
        loadRoleRequests();
        loadAnalyticsRoutes();
    }
    if(viewId === 'writer-view') {
        resetWriterView('char'); // 작가 뷰 초기화 (캐릭터 목록부터)
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
                <span>ID: ${c.id} &gt;</span>
            `;
            li.onclick = () => {
                writerState.charId = c.id;
                writerState.charName = c.name;
                resetWriterView('route');
            };
            list.appendChild(li);
        });
    } catch(e) { console.error(e); }
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
        if(routes.length === 0) list.innerHTML = '<li style="cursor:default; background:#eee;">생성된 루트가 없습니다.</li>';

        routes.forEach(r => {
            const li = document.createElement('li');
            li.innerHTML = `
                <div><strong>${r.title}</strong></div>
                <span>ID: ${r.id} &gt;</span>
            `;
            li.onclick = () => {
                writerState.routeId = r.id;
                writerState.routeTitle = r.title;
                resetWriterView('ep');
            };
            list.appendChild(li);
        });
    } catch(e) { console.error(e); }
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
async function loadWriterEpisodes(routeId) {
    const container = document.getElementById('list-ep');
    container.innerHTML = '';
    try {
        const eps = await api(`/story/routes/${routeId}/episodes`);
        if(eps.length === 0) container.innerHTML = '<p style="padding:10px; color:#999;">에피소드가 없습니다.</p>';

        eps.forEach(ep => {
            const div = document.createElement('div');
            div.className = `ep-card ${ep.isEnding ? 'ending' : ''}`;
            div.innerHTML = `
                <div style="font-weight:bold; margin-bottom:5px;">EP #${ep.id}</div>
                <div style="font-size:0.85em; color:#555;">${ep.text.substring(0, 40)}...</div>
                ${ep.isEnding ? `<div style="color:red; font-size:0.8em; margin-top:5px;">[${ep.endingType}]</div>` : ''} 
            `;
            div.onclick = () => {
                writerState.epId = ep.id;
                writerState.epText = ep.text;
                resetWriterView('choice');
            };
            container.appendChild(div);
        });
    } catch(e) { console.error(e); }
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
document.getElementById('form-create-ep').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const isEnding = fd.get('isEnding') === 'on';

    // 백엔드의 CreateEpisodeRequest DTO에 맞춰 데이터 구성
    const data = {
        routeId: writerState.routeId,
        text: fd.get('text'),
        isEnding: isEnding,
        // 체크되어 있으면 선택된 Enum 값("TRUE", "BAD" 등), 아니면 null 전송
        endingType: isEnding ? fd.get('endingType') : null
    };

    try {
        await api('/story/episodes', 'POST', data);

        // 성공 후 초기화
        e.target.reset();
        toggleEndingSelect(); // UI 상태도 다시 숨김으로 리셋
        loadWriterEpisodes(writerState.routeId); // 목록 갱신

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
            // threshold 유무에 따라 표시 내용 다르게
            let branchInfo = '';
            if (ch.threshold) {
                branchInfo = `<span style="color:purple;">⚖️ 기준 ${ch.threshold}</span> → (성공: EP.${ch.nextEpIfPassId} / 실패: EP.${ch.nextEpIfFailId})`;
            } else {
                branchInfo = `→ EP.${ch.nextEpIfPassId}`;
            }

            li.innerHTML = `
                <div>
                    <strong>${ch.text}</strong>
                    <div style="font-size:0.85em; color:#666; margin-top:4px;">
                        ${ch.minRequiredAffection ? `🔒 잠금 ${ch.minRequiredAffection} | ` : ''}
                        ❤️ ${ch.affectionDelta > 0 ? '+' : ''}${ch.affectionDelta} | 
                        ${branchInfo}
                    </div>
                </div>
            `;
            list.appendChild(li);
        });
    } catch(e) { console.error(e); }
}

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
    try {
        const chars = await api('/game/characters');
        const container = document.getElementById('char-list');
        container.innerHTML = '';
        chars.forEach(c => {
            const div = document.createElement('div');
            div.className = 'char-card';
            div.innerHTML = `
                <h4>${c.name}</h4>
                <p>${c.gender} / ${c.personality}</p>
                <button onclick="startGame(${c.id})">플레이 시작</button>
            `;
            container.appendChild(div);
        });

        // 현재 진행중인 게임 확인
        try {
            const current = await api('/game/current');
            renderGameScreen(current);
        } catch(e) { /* 진행중 게임 없음 */ }

    } catch(err) { console.error(err); }
}

async function startGame(charId) {
    try {
        const state = await api(`/game/start?characterId=${charId}`, 'POST');
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
        html += `<p>결과: <strong>${report.endingLabel}</strong> (${report.endingType})</p>`;
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