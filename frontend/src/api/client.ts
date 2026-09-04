import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Request interceptor to attach JWT Access Token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('taxoryn_access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
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

// Response interceptor for refresh token & error formatting
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Auto-refresh on 401 Unauthorized
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Do not auto-refresh on authentication/session endpoints
      if (
        originalRequest.url?.includes('/auth/login') ||
        originalRequest.url?.includes('/auth/logout') ||
        originalRequest.url?.includes('/auth/refresh')
      ) {
        return Promise.reject(error);
      }

      originalRequest._retry = true;
      const refreshToken = localStorage.getItem('taxoryn_refresh_token');

      if (!refreshToken) {
        localStorage.removeItem('taxoryn_access_token');
        localStorage.removeItem('taxoryn_refresh_token');
        localStorage.removeItem('taxoryn_user');
        localStorage.removeItem('taxoryn_org');
        window.location.href = '/login';
        return Promise.reject(error);
      }

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
        const res = await axios.post(`${API_BASE_URL}/v1/auth/refresh-token`, { refreshToken });
        if (res.data?.success && res.data?.data?.accessToken) {
          const newToken = res.data.data.accessToken;
          localStorage.setItem('taxoryn_access_token', newToken);
          if (res.data.data.refreshToken) {
            localStorage.setItem('taxoryn_refresh_token', res.data.data.refreshToken);
          }
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
        localStorage.removeItem('taxoryn_access_token');
        localStorage.removeItem('taxoryn_refresh_token');
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
