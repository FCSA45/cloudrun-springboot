import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const client = axios.create({
  baseURL: '/api',
  timeout: 10000
});

client.interceptors.request.use((config) => {
  const authStore = useAuthStore();
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => {
    const payload = response.data;
    if (payload && typeof payload === 'object') {
      const { code } = payload;
      if (code !== undefined && code !== 200) {
        const message = payload.msg || payload.message || '请求失败';
        return Promise.reject(new Error(message));
      }
    }
    return payload;
  },
  (error) => {
    const message = error?.response?.data?.msg || error.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

export default client;
