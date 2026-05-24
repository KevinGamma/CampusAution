import http from './http.js'

/**
 * POST /api/v1/users/login
 * @returns {Promise<{ token: string, user: object }>}  LoginResponse.data
 */
export const loginApi = (username, password) =>
  http.post('/api/v1/users/login', { username, password }).then(r => r.data)

/**
 * POST /api/v1/users/register
 * @returns {Promise<{ id, username, balance, role }>}  UserDTO
 */
export const registerApi = (username, password) =>
  http.post('/api/v1/users/register', { username, password }).then(r => r.data)

/**
 * POST /api/v1/users/recharge
 * @returns {Promise<{ id, username, balance, role }>}  updated UserDTO
 */
export const rechargeApi = (amount) =>
  http.post('/api/v1/users/recharge', { amount }).then(r => r.data)

/**
 * PATCH /api/v1/users/profile — update avatar URL and/or bio.
 * Pass only the fields you want to change (null fields are ignored by the server).
 * @param {{ avatarUrl?: string, bio?: string }} payload
 * @returns {Promise<{ id, username, balance, role, avatarUrl, bio }>}  updated UserDTO
 */
export const updateProfileApi = (payload) =>
  http.patch('/api/v1/users/profile', payload).then(r => r.data)
