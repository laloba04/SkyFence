const TOKEN_KEY = 'skyfence_token';
const USER_KEY  = 'skyfence_user';

export function saveAuth(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

export function getUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
}

export function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

export function isAuthenticated() {
    return !!getToken();
}

export function authHeader() {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
}
