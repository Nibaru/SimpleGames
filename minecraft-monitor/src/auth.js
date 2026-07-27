const crypto = require('crypto');

const SESSION_TTL_MS = 7 * 24 * 60 * 60 * 1000; // 7 days
const sessions = new Map();

function createSession() {
  const token = crypto.randomBytes(32).toString('hex');
  sessions.set(token, { createdAt: Date.now() });
  return token;
}

function isValidSession(token) {
  if (!token || !sessions.has(token)) return false;

  const session = sessions.get(token);
  if (Date.now() - session.createdAt > SESSION_TTL_MS) {
    sessions.delete(token);
    return false;
  }

  return true;
}

function authRequired(password) {
  return Boolean(password);
}

function authMiddleware(password) {
  return (req, res, next) => {
    if (!authRequired(password)) return next();
    if (req.path === '/api/login' || req.path === '/api/config') return next();

    const token = req.headers.authorization?.replace(/^Bearer\s+/i, '');
    if (isValidSession(token)) return next();

    if (req.path.startsWith('/api/')) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    next();
  };
}

function login(password, attempt) {
  if (!authRequired(password)) {
    return { token: null, authRequired: false };
  }

  if (attempt !== password) {
    return { error: 'Invalid password' };
  }

  return { token: createSession(), authRequired: true };
}

module.exports = { authMiddleware, login, authRequired, isValidSession };
