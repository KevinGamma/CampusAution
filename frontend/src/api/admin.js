import http from './http.js'

/**
 * ADMIN only — batch-generate student users and their auctions.
 * @returns {Promise<{ usersCreated: number, auctionsCreated: number }>}
 */
export const seedData = (userCount, auctionsPerUser, directPercent = 30, directSaleCount = 0) =>
  http.post('/admin/seed-data', null, {
    params: { userCount, auctionsPerUser, directPercent, directSaleCount },
  }).then(r => r.data)
