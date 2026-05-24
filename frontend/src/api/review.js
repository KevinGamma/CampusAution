import http from './http.js'

/**
 * POST /api/v1/reviews — submit a review for a completed transaction.
 * The reviewee is derived server-side from the order and the caller's identity.
 *
 * @param {{ orderId: number, rating: number, comment?: string }} payload
 * @returns {Promise<Review>}
 */
export const createReview = (payload) =>
  http.post('/api/v1/reviews', payload).then(r => r.data)

/**
 * GET /api/v1/users/{id} — public seller profile (name, avatar, bio, rating, join date).
 * @param {number|string} userId
 * @returns {Promise<SellerProfileResponse>}
 */
export const getSellerProfile = (userId) =>
  http.get(`/api/v1/users/${userId}`).then(r => r.data)

/**
 * GET /api/v1/users/{id}/reviews — all reviews received by a user, newest first.
 * Each entry includes the reviewer's name and avatar.
 * @param {number|string} userId
 * @returns {Promise<ReviewResponse[]>}
 */
export const getSellerReviews = (userId) =>
  http.get(`/api/v1/users/${userId}/reviews`).then(r => r.data)

/**
 * GET /api/v1/auctions?creatorId={id} — active listings published by a specific user.
 * Re-uses the paginated search endpoint; unwraps PageResponse → items array.
 * @param {number|string} userId
 * @returns {Promise<AuctionResponse[]>}
 */
export const getSellerAuctions = (userId) =>
  http.get('/api/v1/auctions', { params: { creatorId: userId } })
    .then(r => r.data?.items ?? r.data)
