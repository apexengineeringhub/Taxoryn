import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// In-Memory Access Token Storage (Never persisted to localStorage/sessionStorage/IndexedDB)
let inMemoryAccessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  inMemoryAccessToken = token;
};

export const getAccessToken = (): string | null => {
  return inMemoryAccessToken;
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
  withCredentials: true, // Automatically transmits HttpOnly refresh cookies across origins/requests
});

// Request interceptor to attach in-memory JWT Access Token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (inMemoryAccessToken && config.headers) {
      config.headers.Authorization = `Bearer ${inMemoryAccessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

const subscribeTokenRefresh = (cb: (token: string) => void) => {
  refreshSubscribers.push(cb);
};

const onRefreshed = (token: string) => {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
};

// Response interceptor for automatic single-flight refresh token rotation & error handling
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Auto-refresh on 401 Unauthorized
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Do not auto-refresh on authentication endpoints to prevent infinite retry loops
      if (
        originalRequest.url?.includes('/auth/login') ||
        originalRequest.url?.includes('/auth/logout') ||
        originalRequest.url?.includes('/auth/refresh') ||
        originalRequest.url?.includes('/auth/forgot-password') ||
        originalRequest.url?.includes('/auth/reset-password')
      ) {
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      if (isRefreshing) {
        return new Promise((resolve) => {
          subscribeTokenRefresh((token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            resolve(apiClient(originalRequest));
          });
        });
      }

      isRefreshing = true;

      try {
        // Send refresh request with withCredentials: true so browser automatically attaches HttpOnly cookie
        const res = await axios.post(
          `${API_BASE_URL}/v1/auth/refresh`,
          {},
          { withCredentials: true }
        );

        if (res.data?.success && res.data?.data?.accessToken) {
          const newToken = res.data.data.accessToken;
          setAccessToken(newToken);
          onRefreshed(newToken);

          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
          }
          return apiClient(originalRequest);
        } else {
          throw new Error('Invalid refresh response');
        }
      } catch (refreshErr) {
        refreshSubscribers = [];
        setAccessToken(null);
        // Clear non-credential cache if present
        localStorage.removeItem('taxoryn_user');
        localStorage.removeItem('taxoryn_org');
        window.location.href = '/login';
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
