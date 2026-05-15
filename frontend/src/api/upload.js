import http from './http.js'

/**
 * POST /api/images/upload — upload a single image file.
 *
 * Sends the file as multipart/form-data with the JWT automatically attached
 * by the Axios request interceptor in http.js.
 *
 * @param {File} file  the File object selected by the user
 * @returns {Promise<string>}  the URL path, e.g. "/api/images/uuid.jpg"
 */
export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/api/images/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(r => r.data)
}
