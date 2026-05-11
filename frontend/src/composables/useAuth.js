import { ref, computed } from 'vue'

// ── Module-level singleton ─────────────────────────────────────────────────────
// These refs live outside any component, so every call to useAuth() shares
// the same reactive state. Reads from localStorage on first module load so
// state survives a page refresh.
const _user  = ref(JSON.parse(localStorage.getItem('ca_user')  ?? 'null'))
const _token = ref(localStorage.getItem('ca_token') ?? '')

export function useAuth() {
  const currentUser = _user
  const token       = _token

  const isLoggedIn = computed(() => !!_user.value)
  const isAdmin    = computed(() => _user.value?.role === 'ADMIN')
  const isStudent  = computed(() => _user.value?.role === 'STUDENT')

  /**
   * Persist session after a successful login response.
   * @param {{ id, username, balance, role }} user
   * @param {string} tk  JWT string
   */
  function login(user, tk) {
    _user.value  = user
    _token.value = tk
    localStorage.setItem('ca_user',  JSON.stringify(user))
    localStorage.setItem('ca_token', tk)
  }

  function logout() {
    _user.value  = null
    _token.value = ''
    localStorage.removeItem('ca_user')
    localStorage.removeItem('ca_token')
  }

  function updateBalance(newBalance) {
    if (_user.value) {
      _user.value.balance = newBalance
      localStorage.setItem('ca_user', JSON.stringify(_user.value))
    }
  }

  return { currentUser, token, isLoggedIn, isAdmin, isStudent, login, logout, updateBalance }
}
