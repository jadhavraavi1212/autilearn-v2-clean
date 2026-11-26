// ============================================================
//  AUTILEARN – main.js  (shared utilities)
// ============================================================

const API_BASE = 'http://localhost:8080/api';

// ── Speak helper (Web Speech API)
function speak(text, rate = 0.85, pitch = 1.2) {
  if (!window.speechSynthesis) return;
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.rate = rate; u.pitch = pitch;
  window.speechSynthesis.speak(u);
}

// ── Points helpers
function getPoints() { return parseInt(sessionStorage.getItem('childPoints') || '0'); }
function addPoints(pts) {
  const total = getPoints() + pts;
  sessionStorage.setItem('childPoints', total);
  const childId = sessionStorage.getItem('childId');
  if (childId) {
    fetch(`${API_BASE}/progress/save`, {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify({ childId: parseInt(childId), moduleId: 0, score: pts, completed: 0 })
    }).catch(() => {});
  }
  return total;
}

// ── Save module progress to backend
function saveProgress(moduleId, score) {
  const childId = sessionStorage.getItem('childId');
  if (!childId) return;
  fetch(`${API_BASE}/progress/save`, {
    method: 'POST', headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ childId: parseInt(childId), moduleId, score, completed: 1 })
  }).catch(() => {});
}

// ── Confetti burst
function confettiBurst() {
  const colors = ['#ff6eb4','#ffa94d','#ffd43b','#51cf66','#339af0','#9775fa'];
  for (let i = 0; i < 50; i++) {
    const el = document.createElement('div');
    el.style.cssText = `position:fixed;width:10px;height:10px;border-radius:50%;
      background:${colors[Math.floor(Math.random()*colors.length)]};
      top:-10px;left:${Math.random()*100}vw;
      animation:confettiFall ${1+Math.random()*2}s ease forwards;
      pointer-events:none;z-index:9999;`;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 3000);
  }
}
if (!document.getElementById('confettiStyle')) {
  const s = document.createElement('style'); s.id='confettiStyle';
  s.textContent = `@keyframes confettiFall{to{transform:translateY(110vh) rotate(720deg);opacity:0}}`;
  document.head.appendChild(s);
}

function animateEl(el, cls) {
  el.classList.add(cls);
  el.addEventListener('animationend', () => el.classList.remove(cls), { once:true });
}

// ── Auth guards
function requireChildAuth() {
  if (!sessionStorage.getItem('childId')) window.location.href = 'login-child.html';
}
function requireParentAuth() {
  if (!sessionStorage.getItem('parentId')) window.location.href = 'login-parent.html';
}

function logout() { sessionStorage.clear(); window.location.href = '../index.html'; }

console.log('%c🧩 AutiLearn loaded!', 'color:#ff6eb4;font-size:1.2rem;font-weight:bold;');
