const API_BASE = (import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '/api' : 'http://localhost:8080/api')).replace(/\/$/, '')

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', ...options.headers },
    })
  } catch {
    throw new Error('Could not reach CampusCare. Check that the backend is running and try again.')
  }

  if (!response.ok) {
    let message = `Request failed (${response.status}). Please try again.`
    try {
      const body = await response.json()
      message = body.message || body.error || message
    } catch {
      // Keep the HTTP fallback when the server does not return JSON.
    }
    const error = new Error(message)
    error.status = response.status
    throw error
  }

  if (response.status === 204) return null
  return response.json()
}

export const getComplaints = () => request('/complaints')
export const createComplaint = (complaint) => request('/complaints', {
  method: 'POST',
  body: JSON.stringify(complaint),
})
export const updateComplaintStatus = (id, status) => request(`/complaints/${id}/status`, {
  method: 'PUT',
  body: JSON.stringify({ status }),
})

export const getAdmin = () => request('/auth/me')
export const registerStudent = (credentials) => request('/auth/register', {
  method: 'POST',
  body: JSON.stringify(credentials),
})
export const loginStudent = (credentials) => request('/auth/student/login', {
  method: 'POST',
  body: JSON.stringify(credentials),
})
export const loginAdmin = (credentials) => request('/auth/login', {
  method: 'POST',
  body: JSON.stringify(credentials),
})
export const logoutAdmin = () => request('/auth/logout', { method: 'POST' })
