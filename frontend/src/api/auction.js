import http from './http.js'

// After the response interceptor unwraps axios → Result<T>,
// each function does .then(r => r.data) to expose the business payload.

/**
 * GET /api/v1/auctions — active listings with optional search, filter, sort, and pagination.
 * Returns the items array directly for backward compatibility with all existing views.
 * Use listActivePaged() if you need total / page / size metadata.
 *
 * @param {object} opts
 * @param {string}  [opts.keyword]    full-text search in title / description
 * @param {string}  [opts.category]   English category key
 * @param {string}  [opts.saleType]   "AUCTION" | "DIRECT"
 * @param {number}  [opts.minPrice]
 * @param {number}  [opts.maxPrice]
 * @param {string}  [opts.startDate]  ISO date "YYYY-MM-DD"
 * @param {string}  [opts.endDate]    ISO date "YYYY-MM-DD"
 * @param {string}  [opts.sortBy]     "currentPrice" | "endTime" | "id"
 * @param {string}  [opts.order]      "ASC" | "DESC"
 * @param {number}  [opts.page]       1-based page number (default 1)
 * @param {number}  [opts.size]       items per page (default 10)
 * @returns {Promise<AuctionResponse[]>}
 */
export const listActive = (opts = {}) => {
  const params = {}
  if (opts.keyword)            params.keyword   = opts.keyword
  if (opts.category)           params.category  = opts.category
  if (opts.saleType)           params.saleType  = opts.saleType
  if (opts.minPrice  != null)  params.minPrice  = opts.minPrice
  if (opts.maxPrice  != null)  params.maxPrice  = opts.maxPrice
  if (opts.startDate)          params.startDate = opts.startDate
  if (opts.endDate)            params.endDate   = opts.endDate
  if (opts.sortBy)             params.sortBy    = opts.sortBy
  if (opts.order)              params.order     = opts.order
  if (opts.page  != null)      params.page      = opts.page
  if (opts.size  != null)      params.size      = opts.size
  return http.get('/api/v1/auctions', { params: Object.keys(params).length ? params : undefined })
    .then(r => r.data?.items ?? r.data)   // unwrap PageResponse → items array
}

/**
 * GET /api/v1/auctions — same as listActive but returns the full PageResponse
 * { items, total, page, size } for components that need pagination metadata.
 * @returns {Promise<{ items: AuctionResponse[], total: number, page: number, size: number }>}
 */
export const listActivePaged = (opts = {}) => {
  const params = {}
  if (opts.keyword)            params.keyword   = opts.keyword
  if (opts.category)           params.category  = opts.category
  if (opts.saleType)           params.saleType  = opts.saleType
  if (opts.minPrice  != null)  params.minPrice  = opts.minPrice
  if (opts.maxPrice  != null)  params.maxPrice  = opts.maxPrice
  if (opts.startDate)          params.startDate = opts.startDate
  if (opts.endDate)            params.endDate   = opts.endDate
  if (opts.sortBy)             params.sortBy    = opts.sortBy
  if (opts.order)              params.order     = opts.order
  if (opts.page  != null)      params.page      = opts.page
  if (opts.size  != null)      params.size      = opts.size
  return http.get('/api/v1/auctions', { params: Object.keys(params).length ? params : undefined })
    .then(r => r.data)
}

/** @returns {Promise<AuctionResponse>} */
export const getAuction = id =>
  http.get(`/api/v1/auctions/${id}`).then(r => r.data)

/**
 * POST /api/v1/auctions/{id}/bids — place a bid.
 * bidderId is now derived server-side from the JWT; only amount is sent.
 * @returns {Promise<Bid>} — HTTP 201 on success
 */
export const placeBid = (auctionId, amount) =>
  http.post(`/api/v1/auctions/${auctionId}/bids`, { amount }).then(r => r.data)

/** STUDENT or ADMIN — creates a new auction. @returns {Promise<AuctionResponse>} — HTTP 201 */
export const createAuction = (payload) =>
  http.post('/api/v1/auctions', payload).then(r => r.data)

/** Owner updates an ACTIVE listing's details. Null fields are left unchanged. */
export const updateAuction = (id, payload) =>
  http.put(`/api/v1/auctions/${id}`, payload).then(r => r.data)

/**
 * PATCH /api/v1/auctions/{id}/status — state transition.
 * Owner cancels an ACTIVE listing, setting its status to CANCELLED.
 */
export const cancelAuction = (id) =>
  http.patch(`/api/v1/auctions/${id}/status`, { status: 'CANCELLED' }).then(r => r.data)

/** ADMIN or owner STUDENT — removes auction and all its bids. */
export const deleteAuction = id =>
  http.delete(`/api/v1/auctions/${id}`).then(r => r.data)

/** Returns all bids for a given auction, newest first. */
export const getAuctionBids = (auctionId) =>
  http.get(`/api/v1/auctions/${auctionId}/bids`).then(r => r.data)

/**
 * POST /api/v1/auctions/{id}/orders — owner accepts a specific bid, closing auction as SOLD.
 * @returns {Promise<Order>} — HTTP 201 on success
 */
export const acceptBid = (auctionId, bidId) =>
  http.post(`/api/v1/auctions/${auctionId}/orders`, { bidId }).then(r => r.data)

/**
 * POST /api/v1/auctions/{id}/orders — owner accepts the current highest bid.
 * @returns {Promise<Order>} — HTTP 201 on success
 */
export const acceptCurrentHighest = (auctionId) =>
  http.post(`/api/v1/auctions/${auctionId}/orders`, { acceptHighest: true }).then(r => r.data)

/**
 * POST /api/v1/auctions/{id}/orders — instant purchase of a DIRECT-sale item.
 * @returns {Promise<Order>} — HTTP 201 on success
 */
export const buyDirect = (auctionId) =>
  http.post(`/api/v1/auctions/${auctionId}/orders`).then(r => r.data)

/** Returns all auctions created by the current user. */
export const listMyItems = () =>
  http.get('/api/v1/users/me/auctions').then(r => r.data)

/** Returns auctions the current user has bid on, each paired with their highest bid. */
export const listMyBids = () =>
  http.get('/api/v1/users/me/bids').then(r => r.data)

/** Returns all items purchased by the current user, newest first. */
export const listPurchased = () =>
  http.get('/api/v1/users/me/orders').then(r => r.data)
