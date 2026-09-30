const API = location.port === '63342' ? 'http://localhost:8080' : '';
const $ = id => document.getElementById(id);
const getToken = () => localStorage.getItem('token');
const getUser = () => localStorage.getItem('username');

function saveSession(data) {
  localStorage.setItem('token', data.token);
  localStorage.setItem('username', data.username);
}
function clearSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
}
function logout() {
  clearSession();
  location.href = 'index.html';
}

function show(el, type, text) { el.className = 'message ' + type; el.textContent = text; }
function clearMsg(el) { el.className = 'message'; el.textContent = ''; }

async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  let res;
  try { res = await fetch(API + path, { ...options, headers }); }
  catch (e) { return { ok: false, status: 0, data: { message: 'Cannot connect to server.' } }; }

  let data = null;
  const text = await res.text();
  if (text) { try { data = JSON.parse(text); } catch (e) { /* not JSON */ } }

  // Token expired or invalid: back to the login page.
  if (res.status === 401 && token) { logout(); }
  return { ok: res.ok, status: res.status, data };
}

function errText(r) {
  if (r.status === 404 && !(r.data && r.data.message)) {
    return 'Server not found (404). Open the app at http://localhost:8080';
  }
  return (r.data && r.data.message) || ('Request failed (' + r.status + ')');
}