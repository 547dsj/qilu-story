import axios from 'axios';
import { useUserStore } from '../store/user';

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('qilu_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      const store = useUserStore();
      store.clearAuth();
      window.dispatchEvent(new CustomEvent('auth-expired'));
    }
    return Promise.reject(error);
  }
);

const handleResponse = async (promise) => {
  const res = await promise;
  if (res.code !== 200) {
    throw new Error(res.message || '接口返回异常');
  }
  return res.data;
};

export const login = (payload) => handleResponse(client.post('/auth/login', payload));
export const registerUser = (payload) => handleResponse(client.post('/auth/register', payload));
export const createStory = (payload) => handleResponse(client.post('/stories', payload));
export const fetchStartNode = (storyId) => handleResponse(client.get(`/stories/${storyId}/start`));
export const fetchNextNode = (storyId, nodeId, choice) =>
  handleResponse(client.get(`/stories/${storyId}/nodes/${nodeId}/next`, { params: { choice } }));
export const getMyStories = () => handleResponse(client.get('/stories/my-stories'));
export const deleteStory = (id) => handleResponse(client.delete(`/stories/${id}`));
export const checkStoryReady = (storyId) => {
    return handleResponse(client.get(`/stories/${storyId}/ready`));
};
