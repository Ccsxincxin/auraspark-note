import axios from 'axios'

function getToken(key: string): string {
  return localStorage.getItem(key) || sessionStorage.getItem(key) || ''
}

function setToken(key: string, value: string) {
  if (localStorage.getItem(key) !== null) {
    localStorage.setItem(key, value)
  } else {
    sessionStorage.setItem(key, value)
  }
}

function removeToken(key: string) {
  localStorage.removeItem(key)
  sessionStorage.removeItem(key)
}

const client = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

client.interceptors.request.use((config) => {
  const token = getToken('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (err.response?.status === 401) {
      const refresh = getToken('refreshToken')
      if (refresh) {
        try {
          const { data } = await axios.post('/api/auth/refresh', { refreshToken: refresh })
          setToken('accessToken', data.data.accessToken)
          setToken('refreshToken', data.data.refreshToken)
          err.config.headers.Authorization = `Bearer ${data.data.accessToken}`
          return client(err.config)
        } catch {
          removeToken('accessToken')
          removeToken('refreshToken')
          window.location.href = '/login'
        }
      } else {
        removeToken('accessToken')
        removeToken('refreshToken')
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  },
)

export default client
