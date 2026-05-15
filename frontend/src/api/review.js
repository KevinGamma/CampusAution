import http from './http.js'

/**
 * POST /api/reviews — submit a review for a completed transaction.
 * The reviewee is derived server-side from the order and the caller's identity.
 *
 * @param {{ orderId: number, rating: number, comment?: string }} payload
 * @returns {Promise<Review>}
 */
export const createReview = (payload) =>
  http.post('/api/reviews', payload).then(r => r.data)

/**
 * GET /users/{id} — public seller profile (name, avatar, bio, rating, join date).
 * @param {number|string} userId
 * @returns {Promise<SellerProfileResponse>}
 */
export const getSellerProfile = (userId) =>
  http.get(`/users/${userId}`).then(r => r.data)

/**
 * GET /users/{id}/reviews — all reviews received by a user, newest first.
 * Each entry includes the reviewer's name and avatar.
 * @param {number|string} userId
 * @returns {Promise<ReviewResponse[]>}
 */
export const getSellerReviews = (userId) =>
  http.get(`/users/${userId}/reviews`).then(r => r.data)

/**
 * GET /auctions?creatorId={id} — active listings published by a specific user.
 * Re-uses the existing search endpoint; passes creatorId as a filter param.
 * @param {number|string} userId
 * @returns {Promise<AuctionResponse[]>}
 */
export const getSellerAuctions = (userId) =>
  http.get('/auctions', { params: { creatorId: userId } }).then(r => r.data)
