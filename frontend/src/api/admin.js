import http from './http.js'

/**
 * ADMIN only — every auction in the system regardless of status.
 * @returns {Promise<Auction[]>}
 */
export const listAll = () =>
  http.get('/api/v1/admin/auctions').then(r => r.data)

/**
 * ADMIN only — batch-generate student users and their auctions.
 * @returns {Promise<{ usersCreated: number, auctionsCreated: number }>}
 */
export const seedData = (userCount, auctionsPerUser, directPercent = 30, directSaleCount = 0) =>
  http.post('/api/v1/admin/seed-data', null, {
    params: { userCount, auctionsPerUser, directPercent, directSaleCount },
  }).then(r => r.data)
