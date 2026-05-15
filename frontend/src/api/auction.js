import http from './http.js'

// After the interceptor unwraps response.data → Result<T>,
// we unwrap once more (.data) to hand callers the raw business object.

/**
 * GET /auctions — active listings with optional search, filter, and sort.
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
  return http.get('/auctions', { params: Object.keys(params).length ? params : undefined }).then(r => r.data)
}

/** @returns {Promise<AuctionResponse>} */
export const getAuction = id =>
  http.get(`/auctions/${id}`).then(r => r.data)

/** @returns {Promise<Bid>} — HTTP 201 on success */
export const placeBid = (auctionId, bidderId, amount) =>
  http.post(`/auctions/${auctionId}/bids`, { bidderId, amount }).then(r => r.data)

/** ADMIN only — every auction regardless of status. @returns {Promise<Auction[]>} */
export const listAll = () =>
  http.get('/auctions/all').then(r => r.data)

/** ADMIN or owner STUDENT — removes auction and all its bids. */
export const deleteAuction = id =>
  http.delete(`/auctions/${id}`).then(r => r.data)

/** STUDENT or ADMIN — creates a new auction. @returns {Promise<AuctionResponse>} — HTTP 201 */
export const createAuction = (payload) =>
  http.post('/auctions', payload).then(r => r.data)

/** Returns all auctions created by the current user. */
export const listMyItems = () =>
  http.get('/auctions/my-items').then(r => r.data)

/** Returns auctions the current user has bid on, each paired with their highest bid. */
export const listMyBids = () =>
  http.get('/auctions/my-bids').then(r => r.data)

/** Returns all bids for a given auction, newest first. */
export const getAuctionBids = (auctionId) =>
  http.get(`/auctions/${auctionId}/bids`).then(r => r.data)

/** Owner accepts a specific bid — closes the auction as SOLD immediately. */
export const acceptBid = (auctionId, bidId) =>
  http.post(`/auctions/${auctionId}/accept-bid/${bidId}`).then(r => r.data)

/** Authenticated user instantly purchases a DIRECT-sale item at its fixed price. */
export const buyDirect = (auctionId) =>
  http.post(`/auctions/${auctionId}/buy`).then(r => r.data)

/** Returns all items purchased by the current user, newest first. */
export const listPurchased = () =>
  http.get('/auctions/purchased').then(r => r.data)

/** Owner updates an ACTIVE listing's details. Null fields are left unchanged. */
export const updateAuction = (id, payload) =>
  http.put(`/auctions/${id}`, payload).then(r => r.data)

/** Owner cancels an ACTIVE listing, setting its status to CANCELLED. */
export const cancelAuction = (id) =>
  http.post(`/auctions/${id}/cancel`).then(r => r.data)

/** Owner accepts the current highest bid without specifying a bid ID — closes auction as SOLD. */
export const acceptCurrentHighest = (auctionId) =>
  http.post(`/auctions/${auctionId}/accept-current-highest`).then(r => r.data)
