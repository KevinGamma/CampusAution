import http from './http.js'

/**
 * POST /users/login
 * @returns {Promise<{ token: string, user: object }>}  LoginResponse.data
 */
export const loginApi = (username, password) =>
  http.post('/users/login', { username, password }).then(r => r.data)

/**
 * POST /users/register
 * @returns {Promise<{ id, username, balance, role }>}  UserDTO
 */
export const registerApi = (username, password) =>
  http.post('/users/register', { username, password }).then(r => r.data)

/**
 * POST /users/recharge
 * @returns {Promise<{ id, username, balance, role }>}  updated UserDTO
 */
export const rechargeApi = (amount) =>
  http.post('/users/recharge', { amount }).then(r => r.data)
